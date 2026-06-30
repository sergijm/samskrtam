CREATE TABLE content.case_endings (
    id                  BIGSERIAL    NOT NULL,
    vowel_type          VARCHAR(20)  NOT NULL,
    gender              VARCHAR(20)  NOT NULL,
    case_type           VARCHAR(20)  NOT NULL,
    number_type         VARCHAR(20)  NOT NULL,
    ending_iast         VARCHAR(20)  NOT NULL,
    ending_devanagari   VARCHAR(20),
    CONSTRAINT pk_case_endings PRIMARY KEY (id),
    CONSTRAINT uq_case_endings UNIQUE (vowel_type, gender, case_type, number_type)
);

COMMENT ON TABLE content.case_endings IS 'Эталонные окончания склонений для всех типов основ (ADR-003)';