/*
 *  Copyright (C) 2010 {Apertum}Projects. web: www.apertum.ru email: info@apertum.ru
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package ru.apertum.qsystem.client.model;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Point;
import javax.swing.JPanel;
import ru.apertum.qsystem.common.QLog;
import ru.apertum.qsystem.common.Uses;
import ru.apertum.qsystem.common.VideoPlayer;
import ru.apertum.qsystem.common.exceptions.ServerException;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;

/**
 * Панель, имеющая фон, загруженный из ресурсов или из файла.
 * Панель может проигрывать фидеофайлы *.mpg, *.jpg.
 * Класс панели с перекрытым методом painComponent()
 * Панель может проигрывать фидеофайлы *.mpg, *.jpg.
 * Для этого используется установленная предварительно на компьютере среда JMF.
 *
 * @author Evgeniy Egorov
 */
public class QPanel extends JPanel {

    private PropertyChangeSupport propertySupport = new PropertyChangeSupport(this);

    /**
     * Для нетбинса.
     */
    public PropertyChangeSupport getPropertySupport() {
        if (propertySupport == null) {
            propertySupport = new PropertyChangeSupport(this);
        }
        return propertySupport;
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        super.addPropertyChangeListener(listener);
        if (listener == null) {
            return;
        }
        getPropertySupport().addPropertyChangeListener(listener);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        super.removePropertyChangeListener(listener);
        if (listener == null || propertySupport == null) {
            return;
        }
        getPropertySupport().removePropertyChangeListener(listener);
    }

    /**
     * Фоновая картинка.
     */
    private transient Image backgroundImage = null;
    private String backgroundImageFile = "";
    public static final String PROP_BACKGROUND_IMG = "backgroundImgage";

    public String getBackgroundImgage() {
        return backgroundImageFile;
    }

    public Image getBackgroundImgageIMG() {
        return backgroundImage;
    }

    /**
     * Установить фоновую картинку.
     *
     * @param resourceName картинка.
     */
    public final void setBackgroundImgage(String resourceName) {
        final String oldValue = resourceName;
        this.backgroundImageFile = resourceName;
        this.backgroundImage = Uses.loadImage(this, resourceName, "");
        getPropertySupport().firePropertyChange(PROP_BACKGROUND_IMG, oldValue, resourceName);
        repaint();
    }

    @Override
    public void paintComponent(Graphics g) {

        if (backgroundImage != null) {
            g.drawImage(backgroundImage, 0, 0, null);
        } else {
            if (getGradient()) {

                Graphics2D g2 = (Graphics2D) g;
                int width = getWidth();
                int height = getHeight();

                // g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);-
                // нет толку на современной машине, по-крайней мере у меня от этой подсказки ничего не меняется

                if (startPoint == null) {
                    startPoint = new Point();
                }
                if (endPoint == null) {
                    endPoint = new Point();
                }
                drawRectangleGradient(g2, width, height);
            } else {
                super.paintComponent(g);
            }
        }
    }

    /**
     * Создает панель и грузит картинку из ресурсов.
     * Если Параметр пустой, то работает как обычная панель.
     *
     * @param resourceName путь к ресурсу в jar-файле или просто к файлу картинки. Может быть пустым.
     */
    public QPanel(String resourceName) {
        if (resourceName != null && !"".equals(resourceName)) {
            setBackgroundImgage(resourceName);
        }
    }

    /**
     * Конструктор по умолчанию.
     */
    public QPanel() {
    }

    private VideoPlayer player = null;

    /**
     * Установит видео на панель и нечнет его проигрывать.
     */
    private void setVideoFile() {
        if (player == null) {
            player = new VideoPlayer();
            removeAll();
            setLayout(new GridLayout(1, 1));
            add(player);
        }
        player.setVideoResource(getVideoFileName());
    }

    /**
     * Обновить размер видео. Если плеер создан.
     */
    public void refreshVideoSize() {
        if (player != null) {
            player.setVideoSize(getNativePosition());
        }
    }

    /**
     * Старт видеоизображения.
     */
    public void startVideo() {
        player.setVideoSize(getNativePosition());
        player.start();
    }

    /**
     * Пауза в видеоизображении.
     */
    public void pouseVideo() {
        player.pause();
    }

    /**
     * Выключение видеоизображения.
     */
    public void closeVideo() {
        if (player != null) {
            player.close();
        }
    }

    public static final String PROP_VIDEO_FILE = "video_file_name";
    /**
     * Файл выводимого видео.
     */
    private String videoFileName = "";

    /**
     * Этот метод установит новое значение файла видео, выведет его на конву.
     * и отцентрирует его в зависимости от установленных выравниваний.
     *
     * @param videoFile видео.
     */
    public void setVideoFileName(String videoFile) {
        final File f = new File(videoFile);
        if (!f.exists()) {
            throw new ServerException("Файл не сущестувет \"" + videoFile + "\"");
        }
        final String oldValue = videoFile;
        this.videoFileName = videoFile;
        getPropertySupport().firePropertyChange(PROP_VIDEO_FILE, oldValue, videoFile);
        setVideoFile();
    }

    /**
     * Файл выводимого видео.
     *
     * @return
     */
    public String getVideoFileName() {
        return videoFileName;
    }

    public static final String PROP_VIDEO_NATIVE_POSITION = "video_native_position";
    /**
     * Расположение выводимого видео.
     */
    private Boolean videoNativePosition = true;

    /**
     * Этот метод установит новое значение расположение видео, выведет его на конву
     * и отцентрирует его в зависимости от установленных выравниваний.
     *
     * @param nativePosition если true, то выводится в первоначальном состоянии, если false, то растянется на всю панель
     */
    public void setNativePosition(Boolean nativePosition) {
        final Boolean oldValue = nativePosition;
        this.videoNativePosition = nativePosition;
        getPropertySupport().firePropertyChange(PROP_VIDEO_NATIVE_POSITION, oldValue, nativePosition);
    }

    /**
     * Расположение выводимого видео.
     */
    public Boolean getNativePosition() {
        return videoNativePosition;
    }

    /**
     * Включение и выключение звука.
     *
     * @param mute выключить или нет.
     */
    public void setMute(boolean mute) {
        try {
            if (player != null) {
                player.getMediaView().getMediaPlayer().setMute(mute);
            }
        } catch (Exception e) {
            QLog.l().logger().error(e);
        }
    }

    public static final String PROP_START_COLOR = "startColor";
    /**
     * Начальный цвет для градиента.
     */
    private Color startColor = Color.red;

    public Color getStartColor() {
        return startColor;
    }

    /**
     * Градиент стартует с цвета.
     *
     * @param startColor цвет.
     */
    public void setStartColor(Color startColor) {
        final Color oldValue = startColor;
        this.startColor = startColor;
        getPropertySupport().firePropertyChange(PROP_START_COLOR, oldValue, startColor);
    }

    public static final String PROP_END_COLOR = "endColor";
    /**
     * Конечный цвет для градиента.
     */
    private Color endColor = Color.blue;

    public Color getEndColor() {
        return endColor;
    }

    /**
     * Конечный ццвет градиента.
     * @param endColor цвет.
     */
    public void setEndColor(Color endColor) {
        final Color oldValue = endColor;
        this.endColor = endColor;
        getPropertySupport().firePropertyChange(PROP_END_COLOR, oldValue, endColor);
    }

    public static final String PROP_START_POINT = "startPoint";
    /**
     * Начальная точка для градиента.
     * Координаты начальной и конечной позиций в которых рисуется градиент.
     */
    private Point startPoint = new Point(0, 0);

    public Point getStartPoint() {
        return startPoint;
    }

    /**
     * Начальная точка для градиента.
     *
     * @param startPoint точка.
     */
    public void setStartPoint(Point startPoint) {
        final Point oldValue = startPoint;
        this.startPoint = startPoint;
        getPropertySupport().firePropertyChange(PROP_START_POINT, oldValue, startPoint);
    }

    public static final String PROP_END_POINT = "endPoint";
    /**
     * Конечная точка для градиента
     * Координаты начальной и конечной позиций в которых рисуется градиент.
     */
    private Point endPoint = new Point(100, 100);

    public Point getEndPoint() {
        return endPoint;
    }

    /**
     * Конечная точка для градиента.
     *
     * @param endPoint точка.
     */
    public void setEndPoint(Point endPoint) {
        final Point oldValue = endPoint;
        this.endPoint = endPoint;
        getPropertySupport().firePropertyChange(PROP_END_POINT, oldValue, endPoint);
    }

    public static final String PROP_CYCLE_FILL_GRADIENT = "cycleFillGradient";
    /**
     * Цикличность заполнения панели градиентным цветом.
     */
    private Boolean cycleFillGradient = true;

    /**
     * Цикличный градиент.
     */
    public void setCycle(Boolean cycleFillGradient) {
        final Boolean oldValue = cycleFillGradient;
        this.cycleFillGradient = cycleFillGradient;
        getPropertySupport().firePropertyChange(PROP_CYCLE_FILL_GRADIENT, oldValue, cycleFillGradient);
    }

    public Boolean getCycle() {
        return cycleFillGradient;
    }

    /**
     * Закрашивать графиентом или нет. По умолчанию не надо градиентом.
     */
    public static final String PROP_GRADIENT = "gradient";
    private Boolean isGradient = false;

    public Boolean getGradient() {
        return isGradient;
    }

    /**
     * Гладиент на панельку.
     */
    public void setGradient(Boolean isGradient) {
        final Boolean oldValue = isGradient;
        this.isGradient = isGradient;
        getPropertySupport().firePropertyChange(PROP_GRADIENT, oldValue, isGradient);
    }

    //Это "СТАНДАРТНОЕ" РИСОВАНИЕ - оно у меня полосит
    private void drawRectangleGradient(Graphics2D g2, int w, int h) {
        GradientPaint gradient = new GradientPaint(startPoint.x, startPoint.y, startColor, endPoint.x, endPoint.y, endColor, cycleFillGradient);
        g2.setPaint(gradient);
        g2.fillRect(0, 0, w, h);
    }
}
