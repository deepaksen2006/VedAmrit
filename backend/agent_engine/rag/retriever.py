from langchain_chroma import Chroma

from agent_engine.rag import config
from agent_engine.rag.ingestion import get_embeddings


class AyurvedaRetriever:

    def __init__(self, persist_directory=None, embeddings=None):
        chroma_path = persist_directory or config.vectorstore_dir()
        embedding_function = embeddings or get_embeddings()

        self.vector_store = Chroma(
            collection_name=config.collection_name(),
            persist_directory=str(chroma_path),
            embedding_function=embedding_function,
        )

    def search(self, query: str, k: int = 5):

        results = self.vector_store.similarity_search(
            query,
            k=k
        )

        return results


if __name__ == "__main__":

    retriever = AyurvedaRetriever()

    query = "Agni digestive fire Mandagni Vishamagni Samagni Tikshnagni"

    results = retriever.search(query, k=5)

    print("\n======================================")
    print("AYURGENIX RAG RETRIEVER TEST")
    print("======================================")

    print(f"\nQuery: {query}\n")

    if not results:
        print("❌ No results found.")
    else:

        for i, result in enumerate(results, start=1):

            print(f"\n--- Result {i} ---")

            print(result.page_content[:1500])

            print("\nSource Information:")

            metadata = result.metadata

            print(
                "Book:",
                metadata.get("source_book", "Unknown")
            )

            print(
                "Page:",
                metadata.get("page", "Unknown")
            )

            print(
                "Author:",
                metadata.get("author", "Unknown")
            )

            print()
