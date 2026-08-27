package sm.selflearn.samskrtam.samcli.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SamcliConfig {

    private MwConfig mw = new MwConfig();

    private CaeConfig cae = new CaeConfig();

    private LemmasConfig lemmas = new LemmasConfig();

    public MwConfig getMw() {
        return mw;
    }

    public void setMw(MwConfig mw) {
        this.mw = mw;
    }

    public CaeConfig getCae() {
        return cae;
    }

    public void setCae(CaeConfig cae) {
        this.cae = cae;
    }

    public LemmasConfig getLemmas() {
        return lemmas;
    }

    public void setLemmas(LemmasConfig lemmas) {
        this.lemmas = lemmas;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MwConfig {
        private String sourceFile;
        private String schema = "cologne_mw";
        private String table = "entries";
        private int batchSize = 1000;
        private boolean truncate = false;

        public String getSourceFile() {
            return sourceFile;
        }

        public void setSourceFile(String sourceFile) {
            this.sourceFile = sourceFile;
        }

        public String getSchema() {
            return schema;
        }

        public void setSchema(String schema) {
            this.schema = schema;
        }

        public String getTable() {
            return table;
        }

        public void setTable(String table) {
            this.table = table;
        }

        public int getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }

        public boolean isTruncate() {
            return truncate;
        }

        public void setTruncate(boolean truncate) {
            this.truncate = truncate;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CaeConfig {
        private String sourceFile;
        private String schema = "cologne_cae";
        private String table = "entries";
        private int batchSize = 1000;
        private boolean truncate = false;

        public String getSourceFile() {
            return sourceFile;
        }

        public void setSourceFile(String sourceFile) {
            this.sourceFile = sourceFile;
        }

        public String getSchema() {
            return schema;
        }

        public void setSchema(String schema) {
            this.schema = schema;
        }

        public String getTable() {
            return table;
        }

        public void setTable(String table) {
            this.table = table;
        }

        public int getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }

        public boolean isTruncate() {
            return truncate;
        }

        public void setTruncate(boolean truncate) {
            this.truncate = truncate;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LemmasConfig {
        private String scriptFile = "etcetera/sql/lingua_index_lemmas.sql";

        public String getScriptFile() {
            return scriptFile;
        }

        public void setScriptFile(String scriptFile) {
            this.scriptFile = scriptFile;
        }
    }
}
