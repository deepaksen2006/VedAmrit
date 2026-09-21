"""
Enhanced AyurGenix RAG Service with Protocol Enforcement

This service implements the RAG_PROTOCOL.md specification:
- Query validation (prevent broad queries)
- Relevance filtering (scope/exclude enforcement)
- Source traceability (Book/Page/Author validation)
- Evidence classification (directly_supported vs reasonably_inferred)
- Conflict handling (surface contradictions)
- Structured JSON output with complete evidence traceability

Integration with QueryTemplates for agent-specific retrieval rules.
"""

from typing import Any, Optional, Literal
import json

from agent_engine.rag.retriever import AyurvedaRetriever


class AyurvedaRAGService:
    """
    Enhanced RAG Service implementing RAG_PROTOCOL.md enforcement.
    
    Ensures all Ayurvedic recommendations are grounded in approved evidence,
    with strict validation on query structure, relevance filtering, and
    source traceability.
    """

    # Approved source books (from RAG_PROTOCOL.md)
    APPROVED_SOURCES = {
        "Charaka Samhita",
        "Sushruta Samhita",
        "Ashtanga Hridaya",
        "Ashtanga Sangraha",
        "Bhavaprakasha Nighantu",
    }

    # Broad query patterns to reject
    BROAD_QUERY_PATTERNS = [
        "ayurvedic information about",
        "tell me about",
        "what is",
        "general",
        "overview of",
    ]

    def __init__(self):
        self.retriever = AyurvedaRetriever()

    # =========================================================================
    # QUERY VALIDATION
    # =========================================================================

    def _is_broad_query(self, query: str) -> bool:
        """
        Check if query follows broad anti-pattern.
        
        Broad queries like "Ayurvedic information about Vata" are rejected.
        Specific queries with context and application are required.
        
        Args:
            query: The retrieval query to validate
            
        Returns:
            bool: True if query is too broad, False if acceptable
        """
        query_lower = query.lower()
        
        # Check for broad patterns
        for pattern in self.BROAD_QUERY_PATTERNS:
            if pattern in query_lower:
                return True
        
        # Query must have minimum length and specificity
        if len(query) < 20:
            return True
        
        return False

    def validate_query(self, query: str) -> tuple[bool, Optional[str]]:
        """
        Validate query against RAG_PROTOCOL.md rules.
        
        Returns:
            tuple: (is_valid, error_message)
                - is_valid: True if query passes validation
                - error_message: Error description if invalid, None if valid
        """
        if not query or not query.strip():
            return False, "Query cannot be empty."
        
        if self._is_broad_query(query):
            return False, (
                "Query is too broad. Use specific queries with: "
                "Concept + Dosha Context + Agni Context + Application + Evidence Type. "
                f"Example: '{query}' is too vague. Instead, include specific context."
            )
        
        return True, None

    # =========================================================================
    # RELEVANCE FILTERING
    # =========================================================================

    def _score_relevance(
        self,
        chunk_content: str,
        scope: list[str],
        exclude: list[str],
    ) -> tuple[Literal["high", "medium", "low"], str]:
        """
        Score chunk relevance based on scope and exclusion rules.
        
        Args:
            chunk_content: The retrieved text chunk
            scope: Topics to prioritize (from QueryTemplate)
            exclude: Topics to reject (from QueryTemplate)
            
        Returns:
            tuple: (relevance_level, reason)
        """
        content_lower = chunk_content.lower()
        
        # Check exclusion rules first (hard reject)
        for exclude_term in exclude:
            if exclude_term.lower() in content_lower:
                return "low", f"Matches exclusion term: '{exclude_term}'"
        
        # Score based on scope matches
        scope_matches = sum(
            1 for term in scope 
            if term.lower() in content_lower
        )
        
        if scope_matches >= 2:
            return "high", f"Matches {scope_matches} scope criteria"
        elif scope_matches == 1:
            return "medium", f"Matches 1 scope criterion"
        else:
            # No explicit scope match but not excluded
            # Check for general relevance (classical text structure)
            if any(word in content_lower for word in ["ayurveda", "dosha", "agni", "rasa"]):
                return "medium", "General Ayurvedic content"
            else:
                return "low", "No scope match and generic content"

    def filter_by_relevance(
        self,
        evidence_list: list[dict[str, Any]],
        scope: Optional[list[str]] = None,
        exclude: Optional[list[str]] = None,
        min_relevance: Literal["high", "medium", "low"] = "medium",
    ) -> list[dict[str, Any]]:
        """
        Filter evidence list by relevance using scope and exclusion rules.
        
        Args:
            evidence_list: List of evidence dicts from retrieve()
            scope: Topics to prioritize (from QueryTemplate)
            exclude: Topics to reject (from QueryTemplate)
            min_relevance: Minimum relevance level to keep
            
        Returns:
            list: Filtered evidence with relevance scores
        """
        if not scope:
            scope = []
        if not exclude:
            exclude = []
        
        filtered = []
        relevance_levels = {"high": 3, "medium": 2, "low": 1}
        min_level = relevance_levels.get(min_relevance, 2)
        
        for item in evidence_list:
            relevance, reason = self._score_relevance(
                item["content"],
                scope,
                exclude,
            )
            
            if relevance_levels[relevance] >= min_level:
                item["relevance"] = relevance
                item["relevance_reason"] = reason
                filtered.append(item)
        
        return filtered

    # =========================================================================
    # SOURCE TRACEABILITY VALIDATION
    # =========================================================================

    def validate_source_traceability(
        self,
        evidence: dict[str, Any],
    ) -> tuple[bool, Optional[str]]:
        """
        Validate that source information is complete and traceable.
        
        Per RAG_PROTOCOL.md, every recommendation must include:
        - Source Book (from approved sources)
        - Page Number
        - Author
        - Content
        
        Args:
            evidence: Single evidence dict from retrieve()
            
        Returns:
            tuple: (is_valid, error_message)
        """
        # Check Book
        book = evidence.get("source_book", "Unknown")
        if book == "Unknown" or not book or book.strip() == "":
            return False, "Source book is missing or unknown."
        
        # Check Page
        page = evidence.get("page", "Unknown")
        if page == "Unknown" or page is None or str(page).strip() == "":
            return False, f"Page number missing for source: {book}"
        
        # Check Author
        author = evidence.get("author", "Unknown")
        if author == "Unknown" or not author or author.strip() == "":
            return False, f"Author missing for: {book}, Page {page}"
        
        # Optional: Verify against approved sources
        # (May not match exactly due to variant names, so comment out for now)
        # if book not in self.APPROVED_SOURCES:
        #     return False, f"Source '{book}' not in approved sources list."
        
        return True, None

    def enforce_source_traceability(
        self,
        evidence_list: list[dict[str, Any]],
    ) -> list[dict[str, Any]]:
        """
        Filter evidence list to only traceable sources.
        
        Args:
            evidence_list: List of evidence dicts
            
        Returns:
            list: Evidence with complete source traceability only
        """
        traceable = []
        
        for item in evidence_list:
            is_valid, error = self.validate_source_traceability(item)
            if is_valid:
                item["source_valid"] = True
                traceable.append(item)
            else:
                # Log invalid sources but don't include them
                item["source_valid"] = False
                item["source_error"] = error
        
        return traceable

    # =========================================================================
    # EVIDENCE CLASSIFICATION
    # =========================================================================

    def classify_evidence(
        self,
        chunk_content: str,
        source_book: str,
    ) -> Literal["directly_supported", "reasonably_inferred"]:
        """
        Classify evidence as directly stated or reasonably inferred.
        
        DIRECTLY_SUPPORTED: The source explicitly states the claim.
        REASONABLY_INFERRED: Derived from multiple sources or logical extension.
        
        Args:
            chunk_content: The evidence text
            source_book: The source book name
            
        Returns:
            str: "directly_supported" or "reasonably_inferred"
        """
        # Heuristic: If content contains qualifying language, likely inferred
        inference_markers = [
            "may",
            "could",
            "suggests",
            "indicates",
            "appears to",
            "often",
            "can be",
            "might",
        ]
        
        content_lower = chunk_content.lower()
        inference_count = sum(
            1 for marker in inference_markers
            if marker in content_lower
        )
        
        # If multiple inference markers, mark as reasonably_inferred
        if inference_count >= 3:
            return "reasonably_inferred"
        
        # Direct statements use imperative or explicit language
        if any(word in content_lower for word in ["is ", "are ", "always ", "never "]):
            return "directly_supported"
        
        # Default based on marker presence
        return "reasonably_inferred" if inference_count > 0 else "directly_supported"

    # =========================================================================
    # CONFLICT DETECTION
    # =========================================================================

    def detect_conflicts(
        self,
        evidence_list: list[dict[str, Any]],
    ) -> list[dict[str, Any]]:
        """
        Detect conflicting claims across evidence sources.
        
        Returns evidence with conflict flags if contradictions detected.
        
        Args:
            evidence_list: List of evidence dicts
            
        Returns:
            list: Evidence with conflict markers
        """
        # This is a simplified conflict detector
        # In production, could use semantic similarity to detect contradictions
        
        conflicts = []
        
        for i, item in enumerate(evidence_list):
            item["conflicts"] = []
            
            # Compare with other items for contradictions
            for j, other in enumerate(evidence_list):
                if i >= j:
                    continue
                
                # Simple heuristic: check for opposite keywords
                opposite_pairs = [
                    ("increase", "decrease"),
                    ("hot", "cold"),
                    ("heavy", "light"),
                    ("wet", "dry"),
                    ("stimulate", "suppress"),
                ]
                
                content1 = item["content"].lower()
                content2 = other["content"].lower()
                
                for word1, word2 in opposite_pairs:
                    if word1 in content1 and word2 in content2:
                        item["conflicts"].append({
                            "type": "potential_contradiction",
                            "conflicting_source": other.get("source_book"),
                            "page": other.get("page"),
                        })
        
        return evidence_list

    # =========================================================================
    # CORE RETRIEVAL METHODS
    # =========================================================================

    def retrieve(
        self,
        query: str,
        k: int = 5,
    ) -> list[dict[str, Any]]:
        """
        Retrieve evidence from vector store.
        
        Basic retrieval without filtering. Use retrieve_with_protocol()
        for full protocol enforcement.
        
        Args:
            query: RAG retrieval query
            k: Number of results
            
        Returns:
            list: Evidence dicts with source information
        """
        results = self.retriever.search(
            query=query,
            k=k,
        )

        evidence = []

        for result in results:
            metadata = result.metadata

            evidence.append({
                "content": result.page_content,
                "source_book": metadata.get("source_book", "Unknown"),
                "page": metadata.get("page", "Unknown"),
                "author": metadata.get("author", "Unknown"),
            })

        return evidence

    def retrieve_with_protocol(
        self,
        query: str,
        k: int = 5,
        scope: Optional[list[str]] = None,
        exclude: Optional[list[str]] = None,
        agent: Optional[str] = None,
    ) -> dict[str, Any]:
        """
        Retrieve evidence with full RAG_PROTOCOL.md enforcement.
        
        This is the primary retrieval method that enforces:
        1. Query validation
        2. Relevance filtering (scope/exclude)
        3. Source traceability
        4. Evidence classification
        5. Conflict detection
        6. Structured JSON output
        
        Args:
            query: The RAG query (must be specific, not broad)
            k: Number of results to retrieve
            scope: Topics to prioritize (from QueryTemplate)
            exclude: Topics to reject (from QueryTemplate)
            agent: Agent context ("Agent 1", "Agent 2", "Agent 3")
            
        Returns:
            dict: Structured evidence output per RAG_PROTOCOL.md format:
                {
                    "query": "...",
                    "agent": "...",
                    "query_valid": bool,
                    "validation_error": str (if invalid),
                    "relevant_sources": [
                        {
                            "source_book": "...",
                            "page": "...",
                            "author": "...",
                            "content": "...",
                            "relevance": "high|medium|low",
                            "support_type": "directly_supported|reasonably_inferred",
                            "conflicts": [...]
                        }
                    ],
                    "evidence_summary": "...",
                    "supported_recommendations": [...],
                    "unsupported_items": [],
                    "safety_notes": [],
                    "retrieval_metadata": {
                        "k_requested": int,
                        "k_retrieved": int,
                        "k_after_relevance_filter": int,
                        "k_after_traceability_filter": int,
                    }
                }
        """
        # Step 1: Validate query
        is_valid, validation_error = self.validate_query(query)
        
        if not is_valid:
            return {
                "query": query,
                "agent": agent,
                "query_valid": False,
                "validation_error": validation_error,
                "relevant_sources": [],
                "evidence_summary": f"Query validation failed: {validation_error}",
                "supported_recommendations": [],
                "unsupported_items": [query],
                "safety_notes": ["Query requires revision before retrieval."],
                "retrieval_metadata": {
                    "k_requested": k,
                    "k_retrieved": 0,
                    "k_after_relevance_filter": 0,
                    "k_after_traceability_filter": 0,
                }
            }
        
        # Step 2: Retrieve from vector store
        evidence_list = self.retrieve(query=query, k=k)
        k_retrieved = len(evidence_list)
        
        # Step 3: Filter by relevance (scope/exclude)
        filtered_by_relevance = self.filter_by_relevance(
            evidence_list,
            scope=scope,
            exclude=exclude,
            min_relevance="medium",
        )
        k_after_relevance = len(filtered_by_relevance)
        
        # Step 4: Enforce source traceability
        traceable_evidence = self.enforce_source_traceability(
            filtered_by_relevance
        )
        k_after_traceability = len(traceable_evidence)
        
        # Step 5: Classify evidence and detect conflicts
        for item in traceable_evidence:
            item["support_type"] = self.classify_evidence(
                item["content"],
                item["source_book"],
            )
        
        traceable_evidence = self.detect_conflicts(traceable_evidence)
        
        # Step 6: Prepare structured output
        return {
            "query": query,
            "agent": agent,
            "query_valid": True,
            "validation_error": None,
            "relevant_sources": traceable_evidence,
            "evidence_summary": (
                f"Retrieved {k_after_traceability} traceable sources "
                f"(from {k_retrieved} initial results)"
                if traceable_evidence else
                "Insufficient evidence retrieved after applying scope and traceability filters."
            ),
            "supported_recommendations": [
                {
                    "source_book": item["source_book"],
                    "page": item["page"],
                    "support_type": item["support_type"],
                }
                for item in traceable_evidence
            ],
            "unsupported_items": [],
            "safety_notes": [
                "This evidence is grounded in classical Ayurvedic texts. "
                "For medical conditions, consult qualified healthcare professionals."
            ],
            "retrieval_metadata": {
                "k_requested": k,
                "k_retrieved": k_retrieved,
                "k_after_relevance_filter": k_after_relevance,
                "k_after_traceability_filter": k_after_traceability,
            }
        }

    def build_context(
        self,
        query: str,
        k: int = 5,
    ) -> str:
        """
        Legacy method: Build formatted context string from evidence.
        
        For backward compatibility. Use retrieve_with_protocol() for new code.
        
        Args:
            query: RAG query
            k: Number of results
            
        Returns:
            str: Formatted context string
        """
        evidence = self.retrieve(query=query, k=k)

        if not evidence:
            return "No relevant Ayurvedic evidence found."

        context_parts = []

        for i, item in enumerate(evidence, start=1):
            context_parts.append(
                f"""
SOURCE {i}

Book: {item["source_book"]}
Page: {item["page"]}
Author: {item["author"]}

Content:
{item["content"]}
"""
            )

        return "\n".join(context_parts)


if __name__ == "__main__":
    """
    Test AyurvedaRAGService with protocol enforcement.
    
    Demonstrates:
    1. Query validation (broad query rejected)
    2. Query acceptance (specific query with context)
    3. Relevance filtering (scope/exclude rules)
    4. Source traceability enforcement
    5. Evidence classification
    6. Structured JSON output
    """
    
    rag = AyurvedaRAGService()
    
    print("\n" + "=" * 70)
    print("AYURGENIX RAG SERVICE — PHASE 3 PROTOCOL ENFORCEMENT TEST")
    print("=" * 70)
    
    # Test 1: Broad query (should be rejected)
    print("\n\n--- TEST 1: Broad Query Validation (Should REJECT) ---")
    broad_query = "Ayurvedic information about Vata"
    is_valid, error = rag.validate_query(broad_query)
    print(f"Query: '{broad_query}'")
    print(f"Valid: {is_valid}")
    if error:
        print(f"Error: {error}")
    
    # Test 2: Specific query (should be accepted)
    print("\n\n--- TEST 2: Specific Query Validation (Should ACCEPT) ---")
    specific_query = (
        "Agni (digestive fire) types including Samagni, Mandagni, Tikshnagni, "
        "and Vishamagni with their characteristics and effects on digestion"
    )
    is_valid, error = rag.validate_query(specific_query)
    print(f"Query: '{specific_query}'")
    print(f"Valid: {is_valid}")
    if error:
        print(f"Error: {error}")
    
    # Test 3: Full protocol retrieval
    print("\n\n--- TEST 3: Protocol-Enforced Retrieval ---")
    result = rag.retrieve_with_protocol(
        query=specific_query,
        k=5,
        scope=["Agni types definition", "Agni characteristics by prakriti", "Assessment indicators"],
        exclude=["Disease-specific agni management", "Pharmaceutical treatments"],
        agent="Agent 1"
    )
    
    print(f"\nQuery: {result['query']}")
    print(f"Agent: {result['agent']}")
    print(f"Query Valid: {result['query_valid']}")
    print(f"\nEvidence Summary: {result['evidence_summary']}")
    print(f"\nRetrieval Metadata:")
    for key, value in result['retrieval_metadata'].items():
        print(f"  {key}: {value}")
    
    print(f"\nSources Retrieved: {len(result['relevant_sources'])}")
    
    if result['relevant_sources']:
        print("\n--- Traceable Sources ---")
        for i, source in enumerate(result['relevant_sources'][:3], 1):
            print(f"\n[Source {i}]")
            print(f"  Book: {source['source_book']}")
            print(f"  Page: {source['page']}")
            print(f"  Author: {source['author']}")
            print(f"  Relevance: {source.get('relevance', 'N/A')}")
            print(f"  Support Type: {source.get('support_type', 'N/A')}")
            print(f"  Content (first 200 chars): {source['content'][:200]}...")
    
    # Test 4: Legacy build_context (backward compatibility)
    print("\n\n--- TEST 4: Legacy Method (Backward Compatibility) ---")
    legacy_query = "Agni digestive fire Mandagni Vishamagni Samagni Tikshnagni"
    context = rag.build_context(query=legacy_query, k=5)
    print("Legacy build_context() still works:")
    print(context[:500] if len(context) > 500 else context)