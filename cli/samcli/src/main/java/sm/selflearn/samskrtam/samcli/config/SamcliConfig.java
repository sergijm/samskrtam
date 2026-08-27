package sm.selflearn.samskrtam.samcli.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SamcliConfig {

    private MwConfig mw = new MwConfig();

    public MwConfig getMw() {
        return mw;
    }

    public void setMw(MwConfig mw) {
        this.mw = mw;
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
}
