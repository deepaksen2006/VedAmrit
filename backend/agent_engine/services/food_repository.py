from pathlib import Path
import csv
from typing import Any


class FoodRepository:
    """
    Loads food data from the demo_foods.csv dataset
    and provides structured food records to Agent 2.
    """

    def __init__(self, csv_path: str | None = None):
        if csv_path is None:
            project_root = Path(__file__).resolve().parents[1]
            csv_path = project_root / "data" / "demo_foods.csv"

        self.csv_path = Path(csv_path)
        if not self.csv_path.exists() and not self.csv_path.is_absolute():
            package_path = Path(__file__).resolve().parents[1] / self.csv_path
            if package_path.exists():
                self.csv_path = package_path
        self.foods = self._load_foods()

    def _load_foods(self) -> list[dict[str, Any]]:
        if not self.csv_path.exists():
            raise FileNotFoundError(
                f"Food dataset not found: {self.csv_path}"
            )

        foods = []

        with open(self.csv_path, "r", encoding="utf-8-sig") as file:
            reader = csv.DictReader(file)

            for row in reader:
                foods.append(self._convert_row(row))

        if not foods:
            raise ValueError("demo_foods.csv is empty.")

        return foods

    def _convert_row(self, row: dict[str, str]) -> dict[str, Any]:
        return {
            "food_id": row["food_id"],
            "food_name": row["food_name"],
            "food_type": row["food_type"],
            "meal_suitability": self._split(row["meal_suitability"]),
            "diet_type": row["diet_type"].strip().lower(),
            "ingredients": self._split(row["ingredients"]),
            "allergens": self._split(row["allergens"]),

            "nutrition": {
                "calories": self._number(row["calories_per_100g"]),
                "protein_g": self._number(row["protein_g"]),
                "carbs_g": self._number(row["carbs_g"]),
                "fat_g": self._number(row["fat_g"]),
                "fiber_g": self._number(row["fiber_g"]),
            },

            "serving": {
                "default_g": self._number(row["default_serving_g"]),
                "min_g": self._number(row["min_serving_g"]),
                "max_g": self._number(row["max_serving_g"]),
            },

            "ayurvedic_attributes": {
                "rasa": self._split(row["rasa"]),
                "guna": self._split(row["guna"]),
                "virya": row["virya"].strip(),
                "vipaka": row["vipaka"].strip(),

                "vata_effect": row["vata_effect"].strip(),
                "pitta_effect": row["pitta_effect"].strip(),
                "kapha_effect": row["kapha_effect"].strip(),

                "agni_suitability": row["agni_suitability"].strip(),
            },

            "best_for": self._split(row["best_for"]),
            "avoid_or_limit_when": self._split(row["avoid_or_limit_when"]),
        }

    @staticmethod
    def _split(value: str) -> list[str]:
        if not value:
            return []

        return [
            item.strip().lower()
            for item in value.split("|")
            if item.strip()
        ]

    @staticmethod
    def _number(value: str) -> float:
        if not value:
            return 0.0

        return float(value)

    def get_all_foods(self) -> list[dict[str, Any]]:
        return self.foods

    def get_food_by_id(self, food_id: str) -> dict[str, Any] | None:
        for food in self.foods:
            if food["food_id"] == food_id:
                return food

        return None
