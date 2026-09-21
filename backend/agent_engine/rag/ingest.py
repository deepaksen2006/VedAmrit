from agent_engine.rag.ingestion import build_vectorstore


def main():

    print("\n=== AyurGenix RAG Ingestion ===\n")
    stats = build_vectorstore()
    print(f"PDFs indexed: {len(stats.source_pdfs)}")
    for source in stats.source_pdfs:
        print(f"- {source}")
    print(f"Pages loaded: {stats.pages_loaded}")
    print(f"Chunks created: {stats.chunks_created}")
    print(f"Chunks added: {stats.chunks_added}")
    print(f"Chunks skipped: {stats.chunks_skipped}")
    print(f"Embedding model: {stats.embedding_model}")
    print(f"Embedding dimension: {stats.embedding_dimension or 'unknown'}")
    print(f"Chunk size: {stats.chunk_size}")
    print(f"Chunk overlap: {stats.chunk_overlap}")
    print(f"Vector store: {stats.vectorstore_path}")
    print("\nRAG vector database is ready.")


if __name__ == "__main__":
    main()
