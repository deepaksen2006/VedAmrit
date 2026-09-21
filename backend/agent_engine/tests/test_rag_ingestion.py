from __future__ import annotations

from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

from langchain_core.documents import Document
from reportlab.pdfgen import canvas

from agent_engine.agents.lifestyle import lifestyle_agent
from agent_engine.integration_service import rag_availability_status
from agent_engine.rag.ingestion import (
    build_vectorstore,
    create_chunks,
    discover_pdf_sources,
    load_pdf_pages,
)
from agent_engine.rag.retriever import AyurvedaRetriever


class FakeEmbeddings:
    def embed_documents(self, texts):
        return [self._embed(text) for text in texts]

    def embed_query(self, text):
        return self._embed(text)

    @staticmethod
    def _embed(text):
        lower = text.lower()
        return [
            float(len(lower) % 17),
            float(lower.count("vata")),
            float(lower.count("agni")),
            float(lower.count("routine") + lower.count("dinacharya")),
        ]


def write_pdf(path: Path, text: str) -> None:
    pdf = canvas.Canvas(str(path))
    pdf.drawString(72, 720, text)
    pdf.showPage()
    pdf.save()


class RagIngestionTests(unittest.TestCase):
    def test_source_discovery_finds_actual_pdfs(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            write_pdf(root / "source-b.pdf", "B")
            write_pdf(root / "source-a.pdf", "A")
            (root / "notes.txt").write_text("ignore", encoding="utf-8")

            sources = discover_pdf_sources(root)

        self.assertEqual([source.name for source in sources], ["source-a.pdf", "source-b.pdf"])

    def test_pdf_loading_preserves_source_and_page_metadata(self):
        with tempfile.TemporaryDirectory() as tmp:
            pdf_path = Path(tmp) / "dinacharya.pdf"
            write_pdf(pdf_path, "Dinacharya routine guidance for Vata and Agni.")

            pages = load_pdf_pages([pdf_path])

        self.assertEqual(len(pages), 1)
        self.assertEqual(pages[0].metadata["source_filename"], "dinacharya.pdf")
        self.assertEqual(pages[0].metadata["source_book"], "dinacharya.pdf")
        self.assertEqual(pages[0].metadata["page"], 1)

    def test_chunk_creation_preserves_metadata_and_chunk_id(self):
        documents = [
            Document(
                page_content="Vata routine and Agni digestion guidance. " * 20,
                metadata={"source_filename": "charaka.pdf", "source_book": "charaka.pdf", "page": 5, "page_number": 5},
            )
        ]

        chunks = create_chunks(documents, size=120, overlap=20)

        self.assertGreater(len(chunks), 1)
        self.assertEqual(chunks[0].metadata["source_filename"], "charaka.pdf")
        self.assertEqual(chunks[0].metadata["page"], 5)
        self.assertIn("chunk_id", chunks[0].metadata)
        self.assertEqual(chunks[0].metadata["chunk_index"], 0)

    def test_vectorstore_creation_and_repeat_ingestion_are_idempotent(self):
        with tempfile.TemporaryDirectory() as books, tempfile.TemporaryDirectory(ignore_cleanup_errors=True) as store:
            write_pdf(Path(books) / "routine.pdf", "Vata dinacharya routine and Agni meal timing guidance.")

            first = build_vectorstore(
                books_path=Path(books),
                persist_directory=Path(store),
                embeddings=FakeEmbeddings(),
            )
            second = build_vectorstore(
                books_path=Path(books),
                persist_directory=Path(store),
                embeddings=FakeEmbeddings(),
            )

        self.assertEqual(first.source_pdfs, ["routine.pdf"])
        self.assertGreater(first.chunks_added, 0)
        self.assertEqual(second.chunks_added, 0)
        self.assertEqual(second.chunks_skipped, first.chunks_created)
        self.assertEqual(first.embedding_dimension, 4)

    def test_retrieval_returns_actual_chunks_with_source_metadata(self):
        with tempfile.TemporaryDirectory() as books, tempfile.TemporaryDirectory(ignore_cleanup_errors=True) as store:
            write_pdf(Path(books) / "routine.pdf", "Vata dinacharya routine and Agni meal timing guidance.")
            build_vectorstore(
                books_path=Path(books),
                persist_directory=Path(store),
                embeddings=FakeEmbeddings(),
            )

            retriever = AyurvedaRetriever(persist_directory=Path(store), embeddings=FakeEmbeddings())
            results = retriever.search("Vata routine Agni", k=2)

        self.assertGreaterEqual(len(results), 1)
        self.assertEqual(results[0].metadata["source_filename"], "routine.pdf")
        self.assertEqual(results[0].metadata["source_book"], "routine.pdf")
        self.assertIn("chunk_id", results[0].metadata)
        self.assertIn("routine", results[0].page_content.lower())

    def test_missing_vectorstore_reports_unavailable(self):
        with tempfile.TemporaryDirectory() as tmp:
            missing = str(Path(tmp) / "missing-vectorstore")
            with patch.dict("os.environ", {"AGENT_ENGINE_RAG_VECTORSTORE_DIR": missing}):
                status = rag_availability_status()

        self.assertFalse(status["available"])
        self.assertEqual(status["error"]["code"], "rag_vector_store_unavailable")

    def test_agent3_uses_available_rag_evidence(self):
        state = {
            "rag_available": True,
            "patient_profile": {
                "age": 34,
                "gender": "female",
                "goal": "wellness",
                "activity_level": "moderate",
                "allergies": [],
                "medicine_allergies": [],
                "food_intolerances": [],
                "foods_to_avoid": [],
                "health_conditions": [],
                "medication_restrictions": [],
                "pregnancy_information": "not_applicable",
            },
            "prakriti_result": {"constitution": "Vata", "primary_dosha": "Vata"},
            "vikriti_result": {"dominant_dosha": "Vata"},
            "agni_result": {"status": "Samagni"},
        }
        evidence = [
            {
                "content": "Daily routine and dinacharya support for Vata.",
                "metadata": {"source_book": "routine.pdf", "page": 1},
            }
        ]

        with patch("agent_engine.agents.lifestyle.get_lifestyle_rag_evidence", return_value=evidence):
            result = lifestyle_agent(state)

        first = result["lifestyle_plan"]["recommendations"][0]
        self.assertEqual(first["ayurvedic_basis"]["source"], "routine.pdf")
        self.assertNotIn("Lifestyle RAG evidence unavailable", " ".join(result["lifestyle_plan"]["safety_flags"]))

    def test_agent3_unavailable_rag_uses_controlled_fallback(self):
        state = {
            "rag_available": False,
            "patient_profile": {
                "age": 34,
                "gender": "female",
                "goal": "wellness",
                "activity_level": "moderate",
                "allergies": [],
                "medicine_allergies": [],
                "food_intolerances": [],
                "foods_to_avoid": [],
                "health_conditions": [],
                "medication_restrictions": [],
                "pregnancy_information": "not_applicable",
            },
            "prakriti_result": {"constitution": "Vata", "primary_dosha": "Vata"},
            "vikriti_result": {"dominant_dosha": "Vata"},
            "agni_result": {"status": "Samagni"},
        }

        with patch("agent_engine.agents.lifestyle.get_lifestyle_rag_evidence") as get_evidence:
            result = lifestyle_agent(state)

        get_evidence.assert_not_called()
        flags = " ".join(result["lifestyle_plan"]["safety_flags"])
        self.assertIn("Lifestyle RAG evidence unavailable", flags)


if __name__ == "__main__":
    unittest.main()
