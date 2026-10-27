import json
from pathlib import Path

from django.contrib.auth import get_user_model
from django.core.management.base import BaseCommand, CommandError
from rest_framework.exceptions import PermissionDenied

from apps.community_care.hospital_import import DEFAULT_NAMESPACE, ImportFailure, import_hospitals


class Command(BaseCommand):
    help = "Create unverified hospital drafts from a supplied CSV without overwriting records or enabling booking."

    def add_arguments(self, parser):
        parser.add_argument("--file", required=True, help="UTF-8 CSV path.")
        parser.add_argument("--dry-run", action="store_true", help="Validate/plan without any database writes.")
        parser.add_argument("--district", help="Case-insensitive exact source-district filter.")
        parser.add_argument("--report", help="New JSON report path; existing files will not be overwritten.")
        parser.add_argument("--source", default=DEFAULT_NAMESPACE, help="Stable source namespace; keep unchanged on repeat imports.")
        parser.add_argument("--actor", type=int, help="Optional existing VedAmrit administrator user ID; otherwise audit as server operator.")

    def handle(self, *args, **options):
        actor = None
        if options["actor"] is not None:
            try:
                actor = get_user_model().objects.get(pk=options["actor"])
            except get_user_model().DoesNotExist as error:
                raise CommandError("Actor does not exist.") from error
        output = None
        try:
            if options["report"]:
                destination = Path(options["report"])
                # Reserve only a new file before beginning; never overwrite the input or other reports.
                destination.parent.mkdir(parents=True, exist_ok=True)
                output = destination.open("x+", encoding="utf-8", newline="\n")

            def write_report(report):
                if output:
                    output.seek(0)
                    json.dump(report, output, ensure_ascii=False, indent=2)
                    output.write("\n")
                    output.truncate()
                    output.flush()

            report = import_hospitals(options["file"], dry_run=options["dry_run"], district=options["district"],
                                      namespace=options["source"], actor=actor, write_report=write_report)
        except (ImportFailure, PermissionDenied, OSError, ValueError) as error:
            raise CommandError(str(error)) from error
        finally:
            if output:
                output.close()
        self.stdout.write(json.dumps({"status": report["status"], **report["summary"]}, sort_keys=True))
