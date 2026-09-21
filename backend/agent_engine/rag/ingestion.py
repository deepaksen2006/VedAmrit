from __future__ import annotations

from dataclasses import dataclass
from hashlib import sha256
from pathlib import Path
import re
from typing import Any, Iterable

from langchain_chroma import Chroma
from langchain_community.document_loaders import PyPDFLoader
from langchain_core.documents import Document
from langchain_huggingface import HuggingFaceEmbeddings
from langchain_text_splitters import RecursiveCharacterTextSplitter

from agent_engine.rag import config


@dataclass(frozen=True)
class RagBuildStats:
    source_pdfs: list[str]
    pages_loaded: int
    chunks_created: int
    chunks_added: int
    chunks_skipped: int
    vectorstore_path: str
    embedding_model: str
    embedding_dimension: int | None
    chunk_size: int
    chunk_overlap: int

    def as_dict(self) -> dict[str, Any]:
        return {
            "source_pdfs": self.source_pdfs,
            "pages_loaded": self.pages_loaded,
            "chunks_created": self.chunks_created,
            "chunks_added": self.chunks_added,
            "chunks_skipped": self.chunks_skipped,
            "vectorstore_path": self.vectorstore_path,
            "embedding_model": self.embedding_model,
            "embedding_dimension": self.embedding_dimension,
            "chunk_size": self.chunk_size,
            "chunk_overlap": self.chunk_overlap,
        }


def discover_pdf_sources(books_path: Path | None = None) -> list[Path]:
    root = books_path or config.books_dir()
    if not root.exists():
        return []
    return sorted(path for path in root.glob("*.pdf") if path.is_file())


def clean_text(value: str) -> str:
    text = str(value or "").replace("\x00", " ")
    text = re.sub(r"[ \t]+", " ", text)
    text = re.sub(r"\n{3,}", "\n\n", text)
    return text.strip()


def load_pdf_pages(pdf_sources: Iterable[Path]) -> list[Document]:
    documents: list[Document] = []
    for pdf_file in pdf_sources:
        loader = PyPDFLoader(str(pdf_file))
        for page in loader.load():
            metadata = dict(page.metadata or {})
            raw_page = metadata.get("page")
            page_number = raw_page + 1 if isinstance(raw_page, int) else raw_page
            metadata.update(
                {
                    "source_filename": pdf_file.name,
                    "source_book": pdf_file.name,
                    "source_path": str(pdf_file),
                    "page": page_number,
                    "page_number": page_number,
                }
            )
            text = clean_text(page.page_content)
            if text:
                documents.append(Document(page_content=text, metadata=metadata))
    return documents


def create_chunks(
    documents: list[Document],
    *,
    size: int | None = None,
    overlap: int | None = None,
) -> list[Document]:
    chunk_size = size or config.chunk_size()
    chunk_overlap = overlap if overlap is not None else config.chunk_overlap()
    splitter = RecursiveCharacterTextSplitter(
        chunk_size=chunk_size,
        chunk_overlap=chunk_overlap,
    )
    chunks = splitter.split_documents(documents)
    source_counts: dict[tuple[str, Any], int] = {}
    enriched: list[Document] = []
    for chunk in chunks:
        metadata = dict(chunk.metadata or {})
        source = str(metadata.get("source_filename") or metadata.get("source_book") or "unknown")
        page = metadata.get("page_number") or metadata.get("page") or "unknown"
        key = (source, page)
        chunk_index = source_counts.get(key, 0)
        source_counts[key] = chunk_index + 1
        content = clean_text(chunk.page_content)
        digest = sha256(f"{source}|{page}|{chunk_index}|{content}".encode("utf-8")).hexdigest()
        chunk_id = f"{Path(source).stem}:{page}:{chunk_index}:{digest[:12]}"
        metadata.update(
            {
                "chunk_id": chunk_id,
                "chunk_index": chunk_index,
                "content_sha256": digest,
            }
        )
        if content:
            enriched.append(Document(page_content=content, metadata=metadata))
    return enriched


def get_embeddings(model_name: str | None = None) -> HuggingFaceEmbeddings:
    return HuggingFaceEmbeddings(model_name=model_name or config.embedding_model_name())


def embedding_dimension(embeddings: Any) -> int | None:
    try:
        vector = embeddings.embed_query("Ayurveda")
    except Exception:
        return None
    return len(vector) if vector is not None else None


def open_vectorstore(
    *,
    persist_directory: Path | None = None,
    embeddings: Any | None = None,
) -> Chroma:
    directory = persist_directory or config.vectorstore_dir()
    directory.mkdir(parents=True, exist_ok=True)
    return Chroma(
        collection_name=config.collection_name(),
        persist_directory=str(directory),
        embedding_function=embeddings or get_embeddings(),
    )


def add_chunks_idempotently(vectorstore: Chroma, chunks: list[Document]) -> tuple[int, int]:
    ids = [str(chunk.metadata["chunk_id"]) for chunk in chunks]
    existing = set()
    if ids:
        existing = set(vectorstore.get(ids=ids).get("ids", []))
    new_pairs = [(chunk_id, chunk) for chunk_id, chunk in zip(ids, chunks) if chunk_id not in existing]
    if new_pairs:
        vectorstore.add_documents(
            documents=[chunk for _, chunk in new_pairs],
            ids=[chunk_id for chunk_id, _ in new_pairs],
        )
    return len(new_pairs), len(chunks) - len(new_pairs)


def build_vectorstore(
    *,
    books_path: Path | None = None,
    persist_directory: Path | None = None,
    embeddings: Any | None = None,
) -> RagBuildStats:
    pdf_sources = discover_pdf_sources(books_path)
    documents = load_pdf_pages(pdf_sources)
    chunks = create_chunks(documents)
    embedding_function = embeddings or get_embeddings()
    vectorstore_path = persist_directory or config.vectorstore_dir()
    vectorstore = open_vectorstore(
        persist_directory=vectorstore_path,
        embeddings=embedding_function,
    )
    chunks_added, chunks_skipped = add_chunks_idempotently(vectorstore, chunks)
    return RagBuildStats(
        source_pdfs=[path.name for path in pdf_sources],
        pages_loaded=len(documents),
        chunks_created=len(chunks),
        chunks_added=chunks_added,
        chunks_skipped=chunks_skipped,
        vectorstore_path=str(vectorstore_path),
        embedding_model=config.embedding_model_name(),
        embedding_dimension=embedding_dimension(embedding_function),
        chunk_size=config.chunk_size(),
        chunk_overlap=config.chunk_overlap(),
    )
