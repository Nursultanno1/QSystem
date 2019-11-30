package ru.apertum.qsystem.common;

/**
 * Модули QSystem из которых состояит веськомплекс.
 * 0-сервер,1-клиент,2-приемная,3-админка,4-киоск,5-сервер хардварных кнопок (17 - редактор табло) 26 - зональники
 */
public enum QModule {
    server,
    client,
    desktop,
    reception,
    admin,
    welcome,
    hardware_buttons,
    tablo_redactor,
    zone_board,
    quser,
    journal,
    unknown;

    public boolean isServer() {
        return this == QModule.server;
    }

    public boolean isClient() {
        return this == QModule.client;
    }
    
    public boolean isDesktop() {
        return this == QModule.desktop;
    }

    public boolean isReception() {
        return this == QModule.reception;
    }

    public boolean isAdminApp() {
        return this == QModule.admin;
    }

    public boolean isWelcome() {
        return this == QModule.welcome;
    }

    public boolean isUB() {
        return this == QModule.hardware_buttons;
    }

    public boolean isZoneBoard() {
        return this == QModule.zone_board;
    }

    public boolean isTabloRedaktor() {
        return this == QModule.tablo_redactor;
    }

    public boolean isUnknown() {
        return this == QModule.unknown;
    }
}
