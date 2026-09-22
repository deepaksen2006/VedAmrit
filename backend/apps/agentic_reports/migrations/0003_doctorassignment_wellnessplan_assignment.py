from django.db import migrations, models
import django.db.models.deletion


class Migration(migrations.Migration):
    dependencies = [
        ("agentic_reports", "0002_patientprofile"),
        ("doctors", "0001_initial"),
    ]

    operations = [
        migrations.CreateModel(
            name="DoctorAssignment",
            fields=[
                (
                    "id",
                    models.BigAutoField(
                        auto_created=True,
                        primary_key=True,
                        serialize=False,
                        verbose_name="ID",
                    ),
                ),
                ("active", models.BooleanField(db_index=True, default=True)),
                ("created_at", models.DateTimeField(auto_now_add=True)),
                ("ended_at", models.DateTimeField(blank=True, null=True)),
                (
                    "doctor",
                    models.ForeignKey(
                        on_delete=django.db.models.deletion.PROTECT,
                        related_name="patient_assignments",
                        to="doctors.doctorprofile",
                    ),
                ),
                (
                    "patient_profile",
                    models.ForeignKey(
                        on_delete=django.db.models.deletion.PROTECT,
                        related_name="doctor_assignments",
                        to="agentic_reports.patientprofile",
                    ),
                ),
            ],
            options={
                "ordering": ["-created_at"],
                "indexes": [
                    models.Index(fields=["doctor", "active"], name="agentic_rep_doctor__e5eb12_idx"),
                    models.Index(fields=["patient_profile", "active"], name="agentic_rep_patient_2c6934_idx"),
                ],
            },
        ),
        migrations.AddField(
            model_name="wellnessplan",
            name="assignment",
            field=models.ForeignKey(
                blank=True,
                null=True,
                on_delete=django.db.models.deletion.PROTECT,
                related_name="wellness_plans",
                to="agentic_reports.doctorassignment",
            ),
        ),
        migrations.AddConstraint(
            model_name="doctorassignment",
            constraint=models.UniqueConstraint(
                condition=models.Q(active=True),
                fields=("patient_profile", "doctor"),
                name="unique_active_patient_doctor_assignment",
            ),
        ),
    ]