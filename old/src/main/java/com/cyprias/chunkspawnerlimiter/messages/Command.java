package com.cyprias.chunkspawnerlimiter.messages;

public final class Command {
    public static class Reload {
        public static final String COMMAND = "reload";
        public static final String ALIAS = "cslreload";
        public static final String PERMISSION = "csl.reload";
        public static final String DESCRIPTION = "Reloads the config file.";

        private Reload() {
            throw new UnsupportedOperationException("This operation is not supported");
        }
    }

    public static class Settings {
        public static final String COMMAND = "settings";
        public static final String ALIAS = "cslsettings";
        public static final String PERMISSION = "csl.settings";
        public static final String DESCRIPTION = "Shows config settings.";

        private Settings() {
            throw new UnsupportedOperationException("This operation is not supported");
        }
    }
    public static class Search {
        public static final String COMMAND = "search";
        public static final String ALIAS = "cslsearch";
        public static final String PERMISSION = "csl.search";
        public static final String DESCRIPTION = "Shows entity search results.";
    }

    public static class Info {
        public static final String COMMAND = "info";
        public static final String ALIAS = "cslinfo";
        public static final String PERMISSION = "csl.info";
        public static final String DESCRIPTION = "Shows config info.";

        private Info() {
            throw new UnsupportedOperationException("This operation is not supported");
        }
    }

    private Command() {
        throw new UnsupportedOperationException("This operation is not supported");
    }
}