import json
import re

from django.core.exceptions import ValidationError


def validate_registration(value):
    # Syntax only. A human must verify the identifier with its issuing council.
    if not re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9 ./_-]{1,78}[A-Za-z0-9]", value) or not any(c.isdigit() for c in value):
        raise ValidationError("Use a 3–80 character registration identifier containing letters/digits and ordinary separators, including at least one digit.")


def validate_phone(value):
    if not re.fullmatch(r"\+?[0-9 ()-]{7,20}", value) or not 7 <= len(re.sub(r"\D", "", value)) <= 15:
        raise ValidationError("Enter a phone number with 7–15 digits and optional leading +, spaces, parentheses or hyphens.")


def validate_postal_code(value):
    if not re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9 -]{1,10}[A-Za-z0-9]", value):
        raise ValidationError("Enter a 3–12 character postal code.")


def validate_string_list(value):
    if not isinstance(value, list) or len(value) > 50 or any(not isinstance(item, str) or not item.strip() or len(item) > 200 for item in value):
        raise ValidationError("Use a list of at most 50 nonblank strings, each at most 200 characters.")


def validate_metadata(value):
    if not isinstance(value, dict) or len(json.dumps(value, ensure_ascii=False)) > 8192:
        raise ValidationError("Use a JSON object of at most 8192 characters. Do not store credentials or private documents here.")
