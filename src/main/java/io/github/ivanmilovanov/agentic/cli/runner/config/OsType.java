package io.github.ivanmilovanov.agentic.cli.runner.config;

/**
 * Тип операционной системы, на которой запущена библиотека.
 */
public enum OsType {
    WINDOWS, MAC, LINUX, OTHER;

    /**
     * Определяет тип ОС по системному свойству {@code os.name}.
     *
     * @return тип текущей ОС
     */
    public static OsType detect() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) return WINDOWS;
        if (os.contains("mac")) return MAC;
        if (os.contains("nix") || os.contains("nux") || os.contains("aix")) return LINUX;
        return OTHER;
    }
}
