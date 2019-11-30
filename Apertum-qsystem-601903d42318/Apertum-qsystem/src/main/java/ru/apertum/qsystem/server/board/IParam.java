package ru.apertum.qsystem.server.board;

import org.dom4j.Element;
import ru.apertum.qsystem.client.forms.FParamsEditor;
import ru.apertum.qsystem.common.QLog;
import ru.apertum.qsystem.common.Uses;
import ru.apertum.qsystem.common.exceptions.ClientException;
import ru.apertum.qsystem.common.exceptions.ServerException;

import javax.swing.border.MatteBorder;
import java.awt.Color;
import java.awt.Font;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Табло. Настройки табло в XML. Каждую настройку надо редактировать в админке. Для этого все параметры надо превратить в элементы списка.
 * Это интерфейс и его реализации для параметров табло в виде элементов списка в JList.
 */
public interface IParam {

    /**
     * В списке надо красиво отображать.
     *
     * @return Представление для списка в редакторе.
     */
    @Override
    String toString();

    /**
     * То что поедет редактироваться в диалог редактирования. Это всегда строка, т.к. редактируем всегда строку.
     *
     * @return Строковое представление параметра для последующего редактирования.
     */
    String getValue();

    String getName();

    /**
     * Требуется для различения примитивных типов данных. Сложные все в строках.
     *
     * @return BOARD_TYPE_INT = 1; BOARD_TYPE_DOUBLE = 2; BOARD_TYPE_STR = 3; BOARD_TYPE_BOOL = 4;
     */
    int getType();

    /**
     * Для строк и сложных, не стандартный типов данных, т.е. списки, шрифт, цвет, кайма и т.д. Для примиривов используется ыкттер примитива.
     *
     * @param value новое значение.
     */
    void setValue(String value);

    void setValue(int value);

    void setValue(double value);

    void setValue(boolean value);

    boolean isReadOnly();

    /**
     * Класс классического итема в списке параметров.
     */
    class Param implements IParam {

        private final Element element;

        public Param(Element element) {
            this.element = element;
        }

        @Override
        public String toString() {
            return getName() + " = " + element.attributeValue(Uses.TAG_BOARD_VALUE);
        }

        @Override
        public String getValue() {
            return element.attributeValue(Uses.TAG_BOARD_VALUE);
        }

        @Override
        public String getName() {
            final String s = FParamsEditor.getLocaleMessage(element.attributeValue(Uses.TAG_BOARD_NAME));
            return s == null ? element.attributeValue(Uses.TAG_BOARD_NAME) : FParamsEditor.getLocaleMessage(element.attributeValue(Uses.TAG_BOARD_NAME));
        }

        @Override
        public int getType() {
            return Integer.parseInt(element.attributeValue(Uses.TAG_BOARD_TYPE));
        }

        @Override
        public void setValue(String value) {
            element.addAttribute(Uses.TAG_BOARD_VALUE, value);
        }

        @Override
        public void setValue(int value) {
            element.addAttribute(Uses.TAG_BOARD_VALUE, String.valueOf(value));
        }

        @Override
        public void setValue(double value) {
            element.addAttribute(Uses.TAG_BOARD_VALUE, String.valueOf(value));
        }

        @Override
        public void setValue(boolean value) {
            element.addAttribute(Uses.TAG_BOARD_VALUE, value ? "1" : "0");
        }

        @Override
        public boolean isReadOnly() {
            return element.attribute(Uses.TAG_BOARD_READ_ONLY) != null;
        }
    }

    /**
     * Класс нового jaxb итема в списке параметров.
     */
    abstract class ParamJaxb<T> implements IParam {

        protected final String name;
        protected int type = -1;

        public ParamJaxb(String name) {
            this.name = name;
            Object value = get();
            switch (value.getClass().getName()) {
                case "java.lang.Integer":
                    type = Uses.BOARD_TYPE_INT;
                    break;
                case "java.lang.Double":
                    type = Uses.BOARD_TYPE_DOUBLE;
                    break;
                case "java.lang.Float":
                    type = Uses.BOARD_TYPE_DOUBLE;
                    break;
                case "java.lang.String":
                    type = Uses.BOARD_TYPE_STR;
                    break;
                case "java.lang.Boolean":
                    type = Uses.BOARD_TYPE_BOOL;
                    break;
                default:
                    type = -1;
                    QLog.l().logger().warn("Неправильный тип \"" + value.getClass().getName() + "\" параметра \"" + value + "\"");
            }
        }

        protected abstract T get();

        protected abstract void set(T value);

        @Override
        public String toString() {
            return name + " = " + get();
        }

        @Override
        public String getValue() {
            return get().toString();
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public int getType() {
            return type;
        }

        @Override
        public void setValue(String strValue) {
            set((T) strValue);
        }

        @Override
        public void setValue(int intValue) {
            set((T) Integer.valueOf(intValue));
        }

        @Override
        public void setValue(double doubleValue) {
            set((T) Double.valueOf(doubleValue));
        }

        @Override
        public void setValue(boolean boolValue) {
            set((T) Boolean.valueOf(boolValue));
        }

        @Override
        public boolean isReadOnly() {
            return type < 0;
        }
    }

    /**
     * Сборник всех реализаций адаптеров для Типов. Каждый Тип адаптируется для редактирования в виде строки.
     */
    @SuppressWarnings({"squid:S1192"})
    enum ParamJaxbImpl {
        NULL(null, null),
        COLOR(Color.class, (field, name, obj) -> new ParamJaxb<String>(name) {
            @Override
            protected String get() {
                try {
                    return new Board.ColorAdapter().marshal((Color) field.get(obj));
                } catch (IllegalAccessException e) {
                    throw new ClientException("\"" + name + "\" is wrong.", e);
                }
            }

            @Override
            protected void set(String value) {
                try {
                    field.set(obj, new Board.ColorAdapter().unmarshal(value));
                } catch (IllegalAccessException e) {
                    throw new ClientException("\"" + name + "\" = \"" + value + "\" is wrong.", e);
                }
            }
        }),
        MATTE_BORDER(MatteBorder.class, (field, name, obj) -> new ParamJaxb<String>(name) {
            @Override
            protected String get() {
                try {
                    return new Board.MatteBorderAdapter().marshal((MatteBorder) field.get(obj));
                } catch (Exception e) {
                    throw new ClientException("\"" + name + "\" is wrong.", e);
                }
            }

            @Override
            protected void set(String value) {
                try {
                    field.set(obj, new Board.MatteBorderAdapter().unmarshal(value));
                } catch (Exception e) {
                    throw new ClientException("\"" + name + "\" = \"" + value + "\" is wrong.", e);
                }
            }
        }),
        FONT(Font.class, (field, name, obj) -> new ParamJaxb<String>(name) {
            @Override
            protected String get() {
                try {
                    return new Board.FontAdapter().marshal((Font) field.get(obj));
                } catch (IllegalAccessException e) {
                    throw new ClientException("\"" + name + "\" is wrong.", e);
                }
            }

            @Override
            protected void set(String value) {
                try {
                    field.set(obj, new Board.FontAdapter().unmarshal(value));
                } catch (IllegalAccessException e) {
                    throw new ClientException("\"" + name + "\" = \"" + value + "\" is wrong.", e);
                }
            }
        }),
        COLORS(Board.Colors.class, (field, name, obj) -> new ParamJaxb<String>(name) {
            @Override
            protected String get() {
                try {
                    Board.Colors colors = (Board.Colors) field.get(obj);
                    return colors.color.stream().map(color -> new Board.ColorAdapter().marshal(color)).collect(Collectors.joining(";"));
                } catch (IllegalAccessException e) {
                    throw new ServerException(e);
                }
            }

            @Override
            protected void set(String value) {
                try {
                    final Board.Colors colors = new Board.Colors();
                    for (String color : value.split("\\s*;\\s*")) {
                        colors.color.add(new Board.ColorAdapter().unmarshal(color));
                    }
                    field.set(obj, colors);
                } catch (IllegalAccessException e) {
                    throw new ClientException("\"" + name + "\" = \"" + value + "\" is wrong.", e);
                }
            }
        }),
        BORDERS(Board.Borders.class, (field, name, obj) -> new ParamJaxb<String>(name) {
            @Override
            protected String get() {
                try {
                    Board.Borders borders = (Board.Borders) field.get(obj);
                    return borders.border.stream().map(border -> {
                        try {
                            return new Board.MatteBorderAdapter().marshal(border);
                        } catch (Exception e) {
                            throw new ServerException(e);
                        }
                    }).collect(Collectors.joining(";"));
                } catch (Exception e) {
                    throw new ClientException("\"" + name + "\" is wrong.", e);
                }
            }

            @Override
            protected void set(String value) {
                try {
                    final Board.Borders borders = new Board.Borders();
                    for (String border : value.split("\\s*;\\s*")) {
                        borders.border.add(new Board.MatteBorderAdapter().unmarshal(border));
                    }
                    field.set(obj, borders);
                } catch (Exception e) {
                    throw new ClientException("\"" + name + "\" = \"" + value + "\" is wrong.", e);
                }
            }
        }),
        SIMPLE_TYPES(null, new IParamAdapter() {
            @Override
            public ParamJaxb makeParameter(Field field, String name, Object obj) {
                return create(field, name, obj);
            }

            private <R> IParam.ParamJaxb<R> create(Field field, String name, Object obj) {
                return new IParam.ParamJaxb<R>(name) {
                    @Override
                    protected R get() {
                        try {
                            return (R) field.get(obj);
                        } catch (Exception e) {
                            throw new ClientException("\"" + name + "\" is wrong.", e);
                        }
                    }

                    @Override
                    protected void set(R value) {
                        try {
                            field.set(obj, value);
                        } catch (Exception e) {
                            throw new ClientException("\"" + name + "\" = \"" + value + "\" is wrong.", e);
                        }
                    }
                };
            }
        });

        @FunctionalInterface
        interface IParamAdapter {
            ParamJaxb makeParameter(Field field, String name, Object obj);
        }

        private final Class clazz;
        private final IParamAdapter paramAdapter;

        ParamJaxbImpl(Class clazz, IParamAdapter paramAdapter) {
            this.clazz = clazz;
            this.paramAdapter = paramAdapter;
        }

        private static IParamAdapter getAdapter(Type type) {
            if (type.equals(Integer.class)
                    || type.equals(String.class)
                    || type.equals(Boolean.class)
                    || type.equals(Double.class)) {
                return SIMPLE_TYPES.paramAdapter;
            }
            return Arrays.stream(ParamJaxbImpl.values()).filter(paramJaxb -> type.equals(paramJaxb.clazz)).findFirst().orElse(NULL).paramAdapter;
        }

        public static ParamJaxb makeParameter(Field field, String name, Object obj) {
            final IParamAdapter adapter = getAdapter(field.getGenericType());
            if (adapter == null) {
                return null;
            } else {
                return adapter.makeParameter(field, name, obj);
            }
        }
    }
}

