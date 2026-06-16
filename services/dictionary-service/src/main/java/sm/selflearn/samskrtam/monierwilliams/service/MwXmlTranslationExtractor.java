
package sm.selflearn.samskrtam.monierwilliams.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import sm.selflearn.samskrtam.monierwilliams.entity.MwEntry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;

@Service
@Slf4j
public class MwXmlTranslationExtractor {

    private final DocumentBuilderFactory factory;

    public MwXmlTranslationExtractor() {
        this.factory = DocumentBuilderFactory.newInstance();
        this.factory.setExpandEntityReferences(true);
        this.factory.setIgnoringElementContentWhitespace(true);
        this.factory.setNamespaceAware(true);
    }

    public String extractTranslation(MwEntry entry) {
        String body = entry.getBody();
        if (body == null || body.isEmpty()) {
            return "";
        }

        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(body)));

            Element bodyElement = doc.getDocumentElement();
            if (!"body".equals(bodyElement.getNodeName())) {
                NodeList bodies = doc.getElementsByTagName("body");
                if (bodies.getLength() > 0) {
                    bodyElement = (Element) bodies.item(0);
                } else {
                    return "";
                }
            }

            // Ищем тег <lex> — это грамматическая информация
            NodeList lexNodes = bodyElement.getElementsByTagName("lex");
            if (lexNodes.getLength() == 0) {
                return extractTextFromBody(bodyElement);
            }

            // Берём последний <lex> (основной)
            Node lexNode = lexNodes.item(lexNodes.getLength() - 1);

            // Идём от <lex> к следующему текстовому узлу
            Node current = lexNode.getNextSibling();
            StringBuilder translation = new StringBuilder();

            while (current != null) {
                // Текстовый узел — это перевод
                if (current.getNodeType() == Node.TEXT_NODE) {
                    String text = current.getTextContent();
                    if (text != null && !text.trim().isEmpty()) {
                        translation.append(text);
                    }
                }

                // Если встретили <ls> или <info> — это конец перевода
                if (current.getNodeType() == Node.ELEMENT_NODE) {
                    String tagName = current.getNodeName();
                    if ("ls".equals(tagName) || "info".equals(tagName)) {
                        break;
                    }

                    // Если внутри есть текст, добавляем
                    if (current.hasChildNodes()) {
                        String innerText = current.getTextContent();
                        if (innerText != null && !innerText.trim().isEmpty()) {
                            // Проверяем, что это не <ab>, <s>, <s1>, <hom>
                            if (!"ab".equals(tagName) && !"s".equals(tagName)
                                    && !"s1".equals(tagName) && !"hom".equals(tagName)) {
                                translation.append(innerText);
                            }
                        }
                    }
                }

                current = current.getNextSibling();
            }

            return cleanTranslation(translation.toString());

        } catch (Exception e) {
            log.warn("Не удалось распарсить XML для записи {}", entry.getRecordIdFull(), e);
            return "";
        }
    }

    /**
     * Извлекает весь текст из body без тегов
     */
    private String extractTextFromBody(Element bodyElement) {
        StringBuilder text = new StringBuilder();
        NodeList children = bodyElement.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);

            if (child.getNodeType() == Node.TEXT_NODE) {
                String content = child.getTextContent();
                if (content != null && !content.trim().isEmpty()) {
                    text.append(content);
                }
            }

            if (child.getNodeType() == Node.ELEMENT_NODE) {
                Element elem = (Element) child;
                String tagName = elem.getTagName();

                if ("ls".equals(tagName) || "info".equals(tagName)) {
                    break;
                }

                if (!"ab".equals(tagName) && !"s".equals(tagName)
                        && !"s1".equals(tagName) && !"hom".equals(tagName)
                        && !"lex".equals(tagName)) {
                    text.append(elem.getTextContent());
                }
            }
        }

        return text.toString();
    }

    /**
     * Очищает и форматирует перевод
     */
    private String cleanTranslation(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }

        String cleaned = raw.replaceAll("\\s+", " ").trim();

        // Берём первое предложение
        int dotPos = cleaned.indexOf(". ");
        if (dotPos > 0 && dotPos < 200) {
            cleaned = cleaned.substring(0, dotPos + 1);
        }

        // Если слишком длинный
        if (cleaned.length() > 300) {
            cleaned = cleaned.substring(0, 300) + "...";
        }

        return cleaned;
    }
}