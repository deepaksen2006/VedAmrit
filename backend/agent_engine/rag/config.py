from __future__ import annotations

import os
from pathlib import Path


RAG_DIR = Path(__file__).resolve().parent
DEFAULT_BOOKS_DIR = RAG_DIR / "books"
DEFAULT_VECTORSTORE_DIR = RAG_DIR / "vectorstore"
DEFAULT_EMBEDDING_MODEL = "sentence-transformers/all-MiniLM-L6-v2"
DEFAULT_CHUNK_SIZE = 1000
DEFAULT_CHUNK_OVERLAP = 150
DEFAULT_COLLECTION_NAME = "ayurveda_sources"


def books_dir() -> Path:
    configured = os.getenv("AGENT_ENGINE_RAG_BOOKS_DIR")
    return Path(configured) if configured else DEFAULT_BOOKS_DIR


def vectorstore_dir() -> Path:
    configured = os.getenv("AGENT_ENGINE_RAG_VECTORSTORE_DIR")
    return Path(configured) if configured else DEFAULT_VECTORSTORE_DIR


def embedding_model_name() -> str:
    return os.getenv("AGENT_ENGINE_RAG_EMBEDDING_MODEL", DEFAULT_EMBEDDING_MODEL)


def chunk_size() -> int:
    return int(os.getenv("AGENT_ENGINE_RAG_CHUNK_SIZE", str(DEFAULT_CHUNK_SIZE)))


def chunk_overlap() -> int:
    return int(os.getenv("AGENT_ENGINE_RAG_CHUNK_OVERLAP", str(DEFAULT_CHUNK_OVERLAP)))


def collection_name() -> str:
    return os.getenv("AGENT_ENGINE_RAG_COLLECTION_NAME", DEFAULT_COLLECTION_NAME)
