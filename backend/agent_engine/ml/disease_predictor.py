from pathlib import Path
import pickle
from typing import Any

import pandas as pd


class DiseasePredictor:
    """
    Symptom-based disease screening model.

    IMPORTANT:
    This model is intended for research/demo screening only.
    It is not a medical diagnostic system.
    """

    def __init__(
        self,
        model_path: str | None = None,
        feature_path: str | None = None,
    ):
        project_root = Path(__file__).resolve().parents[1]

        if model_path is None:
            model_path = project_root / "models" / "disease_decision_tree.pkl"

        if feature_path is None:
            feature_path = project_root / "models" / "disease_features.pkl"

        self.model_path = Path(model_path)
        self.feature_path = Path(feature_path)

        self.model = self._load_pickle(self.model_path)
        self.feature_names = self._load_pickle(self.feature_path)

        self._validate_model()

    @staticmethod
    def _load_pickle(path: Path):
        if not path.exists():
            raise FileNotFoundError(f"File not found: {path}")

        with open(path, "rb") as file:
            return pickle.load(file)

    def _validate_model(self):
        if not hasattr(self.model, "predict"):
            raise ValueError("Loaded object is not a valid prediction model.")

        if len(self.feature_names) != 31:
            raise ValueError(
                f"Expected 31 disease features, "
                f"but found {len(self.feature_names)}."
            )

        if getattr(self.model, "n_features_in_", None) != len(self.feature_names):
            raise ValueError(
                "Model feature count does not match feature-name count."
            )

    def predict(self, symptoms: dict[str, int]) -> dict[str, Any]:
        """
        Predict from a dictionary of 31 symptoms.

        Each symptom must have a value of:
            0 = absent
            1 = present
        """

        missing_features = [
            feature
            for feature in self.feature_names
            if feature not in symptoms
        ]

        if missing_features:
            raise ValueError(
                f"Missing symptom features: {missing_features}"
            )

        invalid_values = {
            feature: symptoms[feature]
            for feature in self.feature_names
            if symptoms[feature] not in (0, 1)
        }

        if invalid_values:
            raise ValueError(
                f"Symptom values must be 0 or 1. "
                f"Invalid values: {invalid_values}"
            )

        # Keep the exact feature order used during training.
        feature_vector = {
            feature: symptoms[feature]
            for feature in self.feature_names
        }

        # Use a DataFrame so sklearn receives the original feature names.
        input_data = pd.DataFrame(
            [feature_vector],
            columns=self.feature_names,
        )

        prediction = self.model.predict(input_data)[0]

        probabilities = {}

        if hasattr(self.model, "predict_proba"):
            probability_values = self.model.predict_proba(input_data)[0]

            probabilities = {
                str(class_name): round(float(probability), 4)
                for class_name, probability in zip(
                    self.model.classes_,
                    probability_values,
                )
            }

        confidence = (
            max(probabilities.values())
            if probabilities
            else None
        )

        return {
            "predicted_condition": str(prediction),
            "confidence": confidence,
            "probabilities": probabilities,
            "screening_status": "screening_only",
            "medical_diagnosis": False,
            "model": "AYUCARE Decision Tree",
            "feature_count": len(self.feature_names),
        }
