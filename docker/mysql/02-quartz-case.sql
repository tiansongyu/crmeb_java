SET FOREIGN_KEY_CHECKS=0;

RENAME TABLE
  `qrtz_blob_triggers` TO `QRTZ_BLOB_TRIGGERS`,
  `qrtz_cron_triggers` TO `QRTZ_CRON_TRIGGERS`,
  `qrtz_fired_triggers` TO `QRTZ_FIRED_TRIGGERS`,
  `qrtz_job_details` TO `QRTZ_JOB_DETAILS`,
  `qrtz_locks` TO `QRTZ_LOCKS`,
  `qrtz_paused_trigger_grps` TO `QRTZ_PAUSED_TRIGGER_GRPS`,
  `qrtz_scheduler_state` TO `QRTZ_SCHEDULER_STATE`,
  `qrtz_simple_triggers` TO `QRTZ_SIMPLE_TRIGGERS`,
  `qrtz_simprop_triggers` TO `QRTZ_SIMPROP_TRIGGERS`,
  `qrtz_triggers` TO `QRTZ_TRIGGERS`;

SET FOREIGN_KEY_CHECKS=1;
