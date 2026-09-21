from __future__ import annotations

from pathlib import Path

from django.core.management.base import BaseCommand, CommandError

from agent_engine.rag.ingestion import build_vectorstore, discover_pdf_sources


class Command(BaseCommand):
    help = "Build the persistent Ayurveda RAG Chroma vector store from source PDFs."

    def add_arguments(self, parser):
        parser.add_argument(
            "--books-dir",
            type=str,
            default=None,
            help="Override AGENT_ENGINE_RAG_BOOKS_DIR for this build.",
        )
        parser.add_argument(
            "--vectorstore-dir",
            type=str,
            default=None,
            help="Override AGENT_ENGINE_RAG_VECTORSTORE_DIR for this build.",
        )

    def handle(self, *args, **options):
        books_path = Path(options["books_dir"]) if options.get("books_dir") else None
        vectorstore_path = Path(options["vectorstore_dir"]) if options.get("vectorstore_dir") else None

        sources = discover_pdf_sources(books_path)
        if not sources:
            raise CommandError("No Ayurveda source PDFs found for RAG ingestion.")

        stats = build_vectorstore(
            books_path=books_path,
            persist_directory=vectorstore_path,
        )

        self.stdout.write(self.style.SUCCESS("Ayurveda RAG vector store is ready."))
        self.stdout.write(f"Source PDFs indexed: {len(stats.source_pdfs)}")
        for source in stats.source_pdfs:
            self.stdout.write(f"- {source}")
        self.stdout.write(f"Pages loaded: {stats.pages_loaded}")
        self.stdout.write(f"Chunks created: {stats.chunks_created}")
        self.stdout.write(f"Chunks added: {stats.chunks_added}")
        self.stdout.write(f"Chunks skipped: {stats.chunks_skipped}")
        self.stdout.write(f"Embedding model: {stats.embedding_model}")
        self.stdout.write(f"Embedding dimension: {stats.embedding_dimension or 'unknown'}")
        self.stdout.write(f"Chunk size: {stats.chunk_size}")
        self.stdout.write(f"Chunk overlap: {stats.chunk_overlap}")
        self.stdout.write(f"Vector-store path: {stats.vectorstore_path}")
