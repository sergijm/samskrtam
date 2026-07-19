/*
 Navicat Premium Data Transfer

 Source Server         : mdm-dev-vm
 Source Server Type    : PostgreSQL
 Source Server Version : 170009 (170009)
 Source Host           : mdm-dev:5432
 Source Catalog        : samskrtam
 Source Schema         : content

 Target Server Type    : PostgreSQL
 Target Server Version : 170009 (170009)
 File Encoding         : 65001

 Date: 20/07/2026 07:24:08
*/


-- ----------------------------
-- Table structure for declension_stems
-- ----------------------------
DROP TABLE IF EXISTS "content"."declension_stems";
CREATE TABLE "content"."declension_stems" (
  "id" uuid NOT NULL DEFAULT gen_random_uuid(),
  "stem_iast" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "vowel_type" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "gender" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "translation_ru" varchar(255) COLLATE "pg_catalog"."default",
  "translation_en" varchar(255) COLLATE "pg_catalog"."default",
  "stem_devanagari" varchar(255) COLLATE "pg_catalog"."default"
)
;
COMMENT ON COLUMN "content"."declension_stems"."id" IS 'Уникальный идентификатор основы склонения';
COMMENT ON COLUMN "content"."declension_stems"."stem_iast" IS 'Название основы в IAST';
COMMENT ON COLUMN "content"."declension_stems"."vowel_type" IS 'Тип гласной основы';
COMMENT ON COLUMN "content"."declension_stems"."gender" IS 'Грамматический род основы';
COMMENT ON COLUMN "content"."declension_stems"."translation_ru" IS 'Перевод основы на русский язык';
COMMENT ON COLUMN "content"."declension_stems"."translation_en" IS 'Перевод основы на английский язык';
COMMENT ON COLUMN "content"."declension_stems"."stem_devanagari" IS 'Название основы в деванагари';
COMMENT ON TABLE "content"."declension_stems" IS 'Таблица для хранения основ склонений';

-- ----------------------------
-- Records of declension_stems
-- ----------------------------
INSERT INTO "content"."declension_stems" VALUES ('17a60c38-2437-4b55-a990-69e46fb09dd8', 'gaurā-', 'AA_STEM', 'FEMININE', 'белая, светлая', 'white, bright', 'गौरा');
INSERT INTO "content"."declension_stems" VALUES ('e53fa87b-0ed7-4424-8cb9-6baa7f6d605b', 'vaidya-', 'A_STEM', 'MASCULINE', 'врач, целитель', 'physician, healer', 'वैद्य');
INSERT INTO "content"."declension_stems" VALUES ('b1e79369-a1e5-43f0-baaa-5e018bc7335a', 'gārhapatya-', 'A_STEM', 'NEUTER', 'относящийся к домашнему огню (гарапатья)', 'relating to the household fire', 'गार्हपत्य');
INSERT INTO "content"."declension_stems" VALUES ('cb0e512c-976e-4452-93c9-32b154a33036', 'chāyā-', 'AA_STEM', 'FEMININE', 'тень, защита, прибежище', 'shade, shadow; protection', 'छाया');
INSERT INTO "content"."declension_stems" VALUES ('b5833966-7d68-4fc5-80e4-7bf73b79cd46', 'bhaginī-', 'II_STEM', 'FEMININE', 'сестра', 'sister', 'भगिनी');
INSERT INTO "content"."declension_stems" VALUES ('32f47c03-40bf-4bbe-9935-0c8304fdff1c', 'vīra-', 'A_STEM', 'MASCULINE', 'герой, храбрец, мужчина', 'hero, brave man, man', 'वीर');
INSERT INTO "content"."declension_stems" VALUES ('b256b657-ac78-49b1-9c34-0fd20e822f2d', 'patha-', 'A_STEM', 'MASCULINE', 'путь, дорога', 'path, way, road', 'पथ');
INSERT INTO "content"."declension_stems" VALUES ('37cf86a7-7f48-4806-a705-97f5c6d32c05', 'sthāna-', 'A_STEM', 'NEUTER', 'место, положение, состояние', 'place, position, condition', 'स्थान');
INSERT INTO "content"."declension_stems" VALUES ('bbccde0e-1081-4bda-b821-dd4abd964358', 'pāra-', 'A_STEM', 'MASCULINE', 'берег, край, предел', 'shore, end, limit', 'पार');
INSERT INTO "content"."declension_stems" VALUES ('ed1a27b0-26f0-4cb1-a074-9c59e0b6e166', 'āditya-', 'A_STEM', 'MASCULINE', 'сын Адити; солнце', 'son of Aditi; the sun', 'आदित्य');
INSERT INTO "content"."declension_stems" VALUES ('febf66ae-e157-4cfb-9d6e-5551002522be', 'jīvana-', 'A_STEM', 'NEUTER', 'жизнь, средство к жизни', 'life, means of living', 'जीवन');
INSERT INTO "content"."declension_stems" VALUES ('ae522edf-6beb-413e-ab5d-965971bd7ae2', 'vana-', 'A_STEM', 'NEUTER', 'лес, роща', 'forest, grove', 'वन');
INSERT INTO "content"."declension_stems" VALUES ('5a8e45a6-393d-48fe-99ee-a5a5fe181b9c', 'ṣaṇḍha-', 'A_STEM', 'MASCULINE', 'евнух, кастрат, бесплодный', 'eunuch, castrated; barren', 'षण्ढ');
INSERT INTO "content"."declension_stems" VALUES ('2291e65f-4336-486a-99ea-4418928b1df9', 'mada-', 'A_STEM', 'MASCULINE', 'опьянение, радость, гордость', 'intoxication, joy, pride', 'मद');
INSERT INTO "content"."declension_stems" VALUES ('598a750c-dcd4-400b-b836-940cc9c7b9cf', 'mukha-', 'A_STEM', 'NEUTER', 'лицо, рот, устье; вход', 'face, mouth, opening; entrance', 'मुख');
INSERT INTO "content"."declension_stems" VALUES ('03978ccd-be51-4b29-941d-ae6f9ea46a6f', 'dāra-', 'A_STEM', 'MASCULINE', 'супруга; жена (мн.ч. — супруги, семья)', 'wife (pl. — wives, family)', 'दार');
INSERT INTO "content"."declension_stems" VALUES ('3acd212e-3520-4858-92ea-d8ce304ee3b3', 'svarga-', 'A_STEM', 'MASCULINE', 'небо, рай, обитель богов', 'heaven, paradise, abode of gods', 'स्वर्ग');
INSERT INTO "content"."declension_stems" VALUES ('9016597d-7789-4377-85dd-33085c6c80b7', 'mitra-', 'A_STEM', 'NEUTER', 'друг (средний род — дружба)', 'friend (neuter — friendship)', 'मित्र');
INSERT INTO "content"."declension_stems" VALUES ('edb40458-cb89-4485-8dab-c92d6449ab74', 'śūra-', 'A_STEM', 'MASCULINE', 'герой, храбрец', 'hero, brave man', 'शूर');
INSERT INTO "content"."declension_stems" VALUES ('695a7123-742f-46c1-82be-0bffb6ef519d', 'duṣṭa-', 'A_STEM', 'MASCULINE', 'испорченный, дурной, злой', 'corrupted, wicked, evil', 'दुष्ट');
INSERT INTO "content"."declension_stems" VALUES ('e8944409-b740-48f6-90ea-066c0508cf06', 'śvetā-', 'AA_STEM', 'FEMININE', 'белая (женская форма)', 'white (feminine)', 'श्वेता');
INSERT INTO "content"."declension_stems" VALUES ('49aa7fa7-996f-456c-909c-21c786b00dc3', 'soma-', 'A_STEM', 'MASCULINE', 'сома (священный напиток; бог луны)', 'soma (sacred drink; moon-god)', 'सोम');
INSERT INTO "content"."declension_stems" VALUES ('b24a422c-4c19-4607-b1af-a3bbba63c86a', 'ratna-', 'A_STEM', 'NEUTER', 'драгоценность, сокровище, жемчужина', 'jewel, treasure, pearl', 'रत्न');
INSERT INTO "content"."declension_stems" VALUES ('08a34c2a-47e7-4261-b8f0-ddbcf25a516b', 'annā-', 'AA_STEM', 'FEMININE', 'пища, еда (женский род)', 'food, meal (feminine)', 'अन्ना');
INSERT INTO "content"."declension_stems" VALUES ('e7171170-10d8-4b20-ac12-0c12664912c3', 'jala-', 'A_STEM', 'NEUTER', 'вода', 'water', 'जल');
INSERT INTO "content"."declension_stems" VALUES ('b64010db-884b-4112-bd19-dc03f3d625db', 'bhāva-', 'A_STEM', 'MASCULINE', 'бытие, становление, состояние, чувство', 'being, becoming, state, feeling', 'भाव');
INSERT INTO "content"."declension_stems" VALUES ('ed448315-4c56-411a-9de3-3bbecad96035', 'nara-', 'A_STEM', 'MASCULINE', 'человек, мужчина', 'man, human being', 'नर');
INSERT INTO "content"."declension_stems" VALUES ('5e053e89-f3c3-4934-b61b-b0606c405d02', 'siṃha-', 'A_STEM', 'MASCULINE', 'лев', 'lion', 'सिंह');
INSERT INTO "content"."declension_stems" VALUES ('a58e9151-1579-42eb-be4d-8bed93f305b7', 'yoga-', 'A_STEM', 'MASCULINE', 'соединение, йога, метод; упряжь', 'union, yoga, method; harness', 'योग');
INSERT INTO "content"."declension_stems" VALUES ('6eebda90-0ab7-4200-9fa9-c6cfe1931b66', 'sūrya-', 'A_STEM', 'MASCULINE', 'солнце, бог солнца Сурья', 'sun, sun-god Sūrya', 'सूर्य');
INSERT INTO "content"."declension_stems" VALUES ('16f77ea4-c189-4195-ae11-4b85d8bf467e', 'ratha-', 'A_STEM', 'MASCULINE', 'колесница, боевая колесница', 'chariot, war-chariot', 'रथ');
INSERT INTO "content"."declension_stems" VALUES ('40091f92-ac03-4793-bbab-b063451e5541', 'aja-', 'A_STEM', 'MASCULINE', 'козёл', 'he-goat', 'अज');
INSERT INTO "content"."declension_stems" VALUES ('ca7818a0-aa2d-4577-8cfa-6716c6e54e12', 'pitṛ-', 'R_STEM', 'FEMININE', 'отец', 'father', 'पितृ');
INSERT INTO "content"."declension_stems" VALUES ('bc2eec4d-90ff-475b-83bf-d7e0f7bb49b0', 'pṛthivī-', 'II_STEM', 'FEMININE', 'земля', 'earth', 'पृथिवी');
INSERT INTO "content"."declension_stems" VALUES ('a5b05f3d-78c4-4882-8514-19446b096eca', 'añjana-', 'A_STEM', 'NEUTER', 'мазь, притирание; коллирий', 'ointment, unguent; collyrium', 'अञ्जन');
INSERT INTO "content"."declension_stems" VALUES ('01587035-4480-4c57-8a7e-b3000be51042', 'tāra-', 'A_STEM', 'MASCULINE', 'звезда', 'star', 'तार');
INSERT INTO "content"."declension_stems" VALUES ('e57645e7-ae57-4ad7-a2a6-c1157c5cb679', 'senā-', 'AA_STEM', 'FEMININE', 'армия, войско', 'army, host', 'सेना');
INSERT INTO "content"."declension_stems" VALUES ('3c57bcdf-d941-40db-af64-40708a8c5554', 'nadī-', 'II_STEM', 'FEMININE', 'река', 'river', 'नदी');
INSERT INTO "content"."declension_stems" VALUES ('f20be878-2a86-45cc-9975-3aed0fd0def3', 'nala-', 'A_STEM', 'MASCULINE', 'Нала (имя царя, герой Махабхараты)', 'Nala (name of a king, hero of Mahabharata)', 'नल');
INSERT INTO "content"."declension_stems" VALUES ('925c0e62-1607-4a0b-b1f4-61ce6626f3b2', 'apūpa-', 'A_STEM', 'MASCULINE', 'лепёшка, хлеб, пирожное', 'cake, bread, pastry', 'अपूप');
INSERT INTO "content"."declension_stems" VALUES ('28c06965-b6c4-40ec-984b-398b8b27b420', 'nīla-', 'A_STEM', 'MASCULINE', 'тёмно-синий, чёрный', 'dark-blue, black', 'नील');
INSERT INTO "content"."declension_stems" VALUES ('0b38bd1c-ed40-447c-adf6-7118ea52b763', 'yajamāna-', 'A_STEM', 'MASCULINE', 'жертвователь (в яджне), хозяин жертвы', 'sacrificer, patron of sacrifice', 'यजमान');
INSERT INTO "content"."declension_stems" VALUES ('fac170cf-cdf5-45ff-a375-2e8fdce63315', 'amara-', 'A_STEM', 'MASCULINE', 'бессмертный, бог', 'immortal, god', 'अमर');
INSERT INTO "content"."declension_stems" VALUES ('f71acb4d-00f6-4444-a32a-798273ed74fe', 'puruṣa-', 'A_STEM', 'MASCULINE', 'человек, мужчина, личность', 'man, male, person', 'पुरुष');
INSERT INTO "content"."declension_stems" VALUES ('68050904-c7d9-43d0-ae22-91a9926e8065', 'samudra-', 'A_STEM', 'MASCULINE', 'океан', 'ocean', 'समुद्र');
INSERT INTO "content"."declension_stems" VALUES ('089ab31b-573f-42f6-972c-80b819ed7921', 'nagarī-', 'II_STEM', 'FEMININE', 'город', 'city', 'नगरी');
INSERT INTO "content"."declension_stems" VALUES ('0e62e850-c288-4d1c-8114-884ee6dc7f29', 'praśna-', 'A_STEM', 'MASCULINE', 'вопрос', 'question', 'प्रश्न');
INSERT INTO "content"."declension_stems" VALUES ('7b59385c-d44e-44ee-a546-7083e512fdd2', 'pṛṣṭa-', 'A_STEM', 'NEUTER', 'спина', 'back', 'पृष्ट');
INSERT INTO "content"."declension_stems" VALUES ('4abb0767-7d62-4308-92f6-43b978f0f1f9', 'arhaṇa-', 'A_STEM', 'NEUTER', 'почитание', 'honoring', 'अर्हण');
INSERT INTO "content"."declension_stems" VALUES ('ecc0fdfd-f0fe-4b74-994c-7fa7953cefe2', 'sūkta-', 'A_STEM', 'NEUTER', 'гимн', 'hymn', 'सूक्त');
INSERT INTO "content"."declension_stems" VALUES ('fd57949a-9db8-47ee-9c7b-5b666cd5c1c4', 'madhyama-', 'A_STEM', 'MASCULINE', 'средний', 'middle', 'मध्यम');
INSERT INTO "content"."declension_stems" VALUES ('cb4e99de-a07e-4f71-80d9-2580059802bc', 'saṅkalpa-', 'A_STEM', 'MASCULINE', 'намерение', 'intention', 'सङ्कल्प');
INSERT INTO "content"."declension_stems" VALUES ('084d85ab-14bf-499b-a81e-8bbcf0c4263c', 'ṛkṣa-', 'A_STEM', 'MASCULINE', 'медведь', 'bear', 'ऋक्ष');
INSERT INTO "content"."declension_stems" VALUES ('e32627a6-11eb-4d49-8879-ca2e4b742f16', 'deśa-', 'A_STEM', 'MASCULINE', 'страна', 'country', 'देश');
INSERT INTO "content"."declension_stems" VALUES ('28eb0ed1-8aad-4bea-9efb-6d1ad6d554c5', 'aṅga-', 'A_STEM', 'NEUTER', 'часть тела', 'limb', 'अङ्ग');
INSERT INTO "content"."declension_stems" VALUES ('805472b4-6459-44cf-91df-2a64a86b33ac', 'pravāha-', 'A_STEM', 'MASCULINE', 'течение, поток', 'flow, stream', 'प्रवाह');
INSERT INTO "content"."declension_stems" VALUES ('02b08087-dcb6-4c99-a1f0-f1f2b9eef36b', 'agni-', 'II_STEM', 'FEMININE', 'огонь (жен. род в ритуале)', 'fire (fem. in ritual contexts)', 'अग्नि');
INSERT INTO "content"."declension_stems" VALUES ('33e13212-9102-4f26-a2bf-fb0203497f42', 'makara-', 'A_STEM', 'MASCULINE', 'крокодил', 'crocodile', 'मकर');
INSERT INTO "content"."declension_stems" VALUES ('a9529dbd-7c08-43c3-83bf-2832684cc79f', 'mūla-', 'A_STEM', 'NEUTER', 'корень', 'root', 'मूल');
INSERT INTO "content"."declension_stems" VALUES ('980dafa3-3c88-44f5-9574-3440ca36b985', 'vāsa-', 'A_STEM', 'MASCULINE', 'одежда, жилище', 'garment, dwelling', 'वास');
INSERT INTO "content"."declension_stems" VALUES ('41a2d07e-3021-4134-b9bc-1311ea0f8f73', 'gaṇa-', 'A_STEM', 'MASCULINE', 'группа, множество', 'group, multitude', 'गण');
INSERT INTO "content"."declension_stems" VALUES ('d73ea88f-5a1f-4f78-9c88-eef8b5f4d7be', 'deva-', 'A_STEM', 'MASCULINE', 'бог', 'god', 'देव');
INSERT INTO "content"."declension_stems" VALUES ('2f7e56fe-ccb3-4557-a830-050f2a4d128f', 'dravya-', 'A_STEM', 'NEUTER', 'вещество, объект', 'substance, object', 'द्रव्य');
INSERT INTO "content"."declension_stems" VALUES ('f60aacd4-5faf-4f87-a545-4ab9263c9582', 'ghoṣa-', 'A_STEM', 'MASCULINE', 'звук, шум', 'sound, noise', 'घोष');
INSERT INTO "content"."declension_stems" VALUES ('1f244e23-13ec-491e-8b64-537ebaa8b3e8', 'garbha-', 'A_STEM', 'MASCULINE', 'плод, зародыш', 'womb, embryo', 'गर्भ');
INSERT INTO "content"."declension_stems" VALUES ('932128e1-7dd5-49e3-a421-13f83de77ab0', 'jñāna-', 'A_STEM', 'NEUTER', 'знание', 'knowledge', 'ज्ञान');
INSERT INTO "content"."declension_stems" VALUES ('9fc38e76-0216-42aa-97e6-36dfacb350e0', 'dvija-', 'A_STEM', 'MASCULINE', 'дваждырождённый', 'twice-born', 'द्विज');
INSERT INTO "content"."declension_stems" VALUES ('cd1c0794-1fd9-4771-9e9f-9e65b0849706', 'yajña-', 'A_STEM', 'MASCULINE', 'жертвоприношение', 'sacrifice', 'यज्ञ');
INSERT INTO "content"."declension_stems" VALUES ('811e7854-490a-4712-b0c0-f03eaa2875f1', 'agāra-', 'A_STEM', 'MASCULINE', 'дом', 'house', 'अगार');
INSERT INTO "content"."declension_stems" VALUES ('b814d666-ddbf-42ab-a537-fb934fb56037', 'pṛṣṭha-', 'A_STEM', 'NEUTER', 'спина', 'back', 'पृष्ठ');
INSERT INTO "content"."declension_stems" VALUES ('2dd78f5a-c9ad-4457-ad93-0c60330188d8', 'kanyā-', 'AA_STEM', 'FEMININE', 'девушка', 'girl', 'कन्या');
INSERT INTO "content"."declension_stems" VALUES ('73f8ec55-fb84-4450-bb82-c232a0eead1f', 'hṛdaya-', 'A_STEM', 'NEUTER', 'сердце', 'heart', 'हृदय');
INSERT INTO "content"."declension_stems" VALUES ('b0445707-a08e-4066-8d6a-e5894c9844db', 'antarāla-', 'A_STEM', 'NEUTER', 'середина', 'middle', 'अन्तराल');
INSERT INTO "content"."declension_stems" VALUES ('10a2d9e3-6dd5-49b6-9a9e-e7548676b298', 'mṛdaṅga-', 'A_STEM', 'MASCULINE', 'барабан', 'drum', 'मृदङ्ग');
INSERT INTO "content"."declension_stems" VALUES ('6fe6c8f4-ea1d-436d-905f-5f096701014e', 'hamsa-', 'A_STEM', 'MASCULINE', 'лебедь', 'swan', 'हंस');
INSERT INTO "content"."declension_stems" VALUES ('75de4197-5e84-4535-aa48-10cb0009fbe4', 'snātaka-', 'A_STEM', 'MASCULINE', 'учёный брахман', 'learned brahmin', 'स्नातक');
INSERT INTO "content"."declension_stems" VALUES ('e21642fe-b799-4bd9-b15a-152ed83d3623', 'divasa-', 'A_STEM', 'MASCULINE', 'день', 'day', 'दिवस');
INSERT INTO "content"."declension_stems" VALUES ('f7974a81-fe9a-405e-a5f1-02b549a5df3b', 'prajā-', 'AA_STEM', 'FEMININE', 'потомство, народ', 'offspring, people', 'प्रजा');
INSERT INTO "content"."declension_stems" VALUES ('c7574bcc-6a4f-41af-a639-2563cb9f0f04', 'āśā-', 'AA_STEM', 'FEMININE', 'надежда', 'hope', 'आशा');
INSERT INTO "content"."declension_stems" VALUES ('3dd50653-839b-4852-86a8-6ee445d6e594', 'para-', 'A_STEM', 'MASCULINE', 'другой, высший', 'other, supreme', 'पर');
INSERT INTO "content"."declension_stems" VALUES ('b74eedaa-4c4e-4282-b3aa-3928dd916718', 'indra-', 'A_STEM', 'MASCULINE', 'Индра', 'Indra', 'इन्द्र');
INSERT INTO "content"."declension_stems" VALUES ('61edb9eb-db38-4215-9ba9-2d38cfb35fc7', 'śukra-', 'A_STEM', 'MASCULINE', 'Венера', 'Venus', 'शुक्र');
INSERT INTO "content"."declension_stems" VALUES ('e54a36ee-00f0-4c39-9d9e-4397d0360b54', 'kāla-', 'A_STEM', 'MASCULINE', 'время', 'time', 'काल');
INSERT INTO "content"."declension_stems" VALUES ('5533b8a2-9760-45a1-83de-b071508f1107', 'bīja-', 'A_STEM', 'NEUTER', 'зерно, семя', 'grain, seed', 'बीज');
INSERT INTO "content"."declension_stems" VALUES ('bb253984-0b86-46d8-85e5-34cb97aea7cb', 'tīra-', 'A_STEM', 'NEUTER', 'берег', 'shore', 'तीर');
INSERT INTO "content"."declension_stems" VALUES ('2bd73d93-d03a-402f-92ff-fd9d9ef1542c', 'nalopākhyāna-', 'A_STEM', 'NEUTER', 'сказание о Нале', 'story of Nala', 'नलोपाख्यान');
INSERT INTO "content"."declension_stems" VALUES ('e1769996-00fa-4d3a-8826-2e292a4fabc6', 'mṛga-', 'A_STEM', 'MASCULINE', 'олень', 'deer', 'मृग');
INSERT INTO "content"."declension_stems" VALUES ('f91bac4b-32ee-446f-8cd3-742d2c297307', 'caraṇa-', 'A_STEM', 'NEUTER', 'нога', 'foot', 'चरण');
INSERT INTO "content"."declension_stems" VALUES ('7ffbb375-a20b-4962-a9e9-121858ddffee', 'aśva-', 'A_STEM', 'MASCULINE', 'конь', 'horse', 'अश्व');
INSERT INTO "content"."declension_stems" VALUES ('adfc4e84-eae7-4acd-b790-f2b10a01f48b', 'bala-', 'A_STEM', 'NEUTER', 'сила', 'strength', 'बल');
INSERT INTO "content"."declension_stems" VALUES ('d1521f4e-c9f7-4cd5-bb4e-6f2a2c9b9b92', 'asura-', 'A_STEM', 'MASCULINE', 'демон', 'demon', 'असुर');
INSERT INTO "content"."declension_stems" VALUES ('ac2937d6-b9da-4ba6-ac0c-c2ea0bd4d42a', 'daiva-', 'A_STEM', 'NEUTER', 'судьба', 'destiny', 'दैव');
INSERT INTO "content"."declension_stems" VALUES ('f6707b8e-3d22-4ea0-a93b-46381037f46d', 'tṛṇa-', 'A_STEM', 'NEUTER', 'трава', 'grass', 'तृण');
INSERT INTO "content"."declension_stems" VALUES ('ff60a3b5-90ed-4fd4-a659-9047c6c5add0', 'vatsa-', 'A_STEM', 'MASCULINE', 'телёнок', 'calf', 'वत्स');
INSERT INTO "content"."declension_stems" VALUES ('1bfbb660-1787-4763-99e6-f69bc3e12107', 'pūrṇa-', 'A_STEM', 'MASCULINE', 'полный', 'full', 'पूर्ण');
INSERT INTO "content"."declension_stems" VALUES ('d809686f-157f-49d1-b517-69d32bddeec7', 'pādapa-', 'A_STEM', 'MASCULINE', 'дерево', 'tree', 'पादप');
INSERT INTO "content"."declension_stems" VALUES ('a3301542-092e-48c7-bc76-5cc3c94cdc08', 'nārī-', 'II_STEM', 'FEMININE', 'женщина', 'woman', 'नारी');
INSERT INTO "content"."declension_stems" VALUES ('3c0bd896-7c70-4d2d-bd49-13e3ad811114', 'yodha-', 'A_STEM', 'MASCULINE', 'воин, боец', 'warrior, fighter', 'योध');
INSERT INTO "content"."declension_stems" VALUES ('f04183a7-e906-47c6-9728-1c070b668fd2', 'nakṣatra-', 'A_STEM', 'NEUTER', 'звезда, созвездие, накшатра', 'star, constellation, lunar mansion', 'नक्षत्र');
INSERT INTO "content"."declension_stems" VALUES ('cd4bfce5-e181-42b6-aca9-fd4781b04867', 'eṇī-', 'II_STEM', 'FEMININE', 'газель, самка антилопы', 'gazelle, female antelope', 'एणी');
INSERT INTO "content"."declension_stems" VALUES ('b32b08a3-e14d-44ea-9cc7-026d4bf35703', 'prāṇa-', 'A_STEM', 'MASCULINE', 'дыхание, жизнь, энергия', 'breath, life, energy', 'प्राण');
INSERT INTO "content"."declension_stems" VALUES ('112775f3-9beb-4fcc-b7c9-d803d769a048', 'gaja-', 'A_STEM', 'MASCULINE', 'слон', 'elephant', 'गज');
INSERT INTO "content"."declension_stems" VALUES ('c2b4b372-e5b8-489b-ba9c-2bc85bc307dd', 'bhrātṛ-', 'R_STEM', 'FEMININE', 'сестра (редко); также — мать (устар.)', 'sister (rare); also mother (arch.)', 'भ्रातृ');
INSERT INTO "content"."declension_stems" VALUES ('d029c42d-009d-4863-9daf-05b1a1243493', 'tata-', 'A_STEM', 'MASCULINE', 'отец', 'father', 'तत');
INSERT INTO "content"."declension_stems" VALUES ('0016742e-dd77-4264-8d95-ec62072e8fef', 'sita-', 'A_STEM', 'MASCULINE', 'белый, светлый', 'white, bright', 'सित');
INSERT INTO "content"."declension_stems" VALUES ('ace6e542-e434-4e2e-96a5-5963fc526829', 'śūla-', 'A_STEM', 'NEUTER', 'копье, острие, колючка', 'spear, spike, thorn', 'शूल');
INSERT INTO "content"."declension_stems" VALUES ('c1184759-0c5f-438b-b3be-4da69457fbdd', 'pratiṣṭhā-', 'AA_STEM', 'FEMININE', 'основание, фундамент, утверждение', 'foundation, base, establishment', 'प्रतिष्ठा');
INSERT INTO "content"."declension_stems" VALUES ('362eebf7-1e3c-4a40-bc40-c90eafcb94ff', 'gāyatrī-', 'II_STEM', 'FEMININE', 'Гаятри (имя богини, священный стихотворный размер)', 'Gayatri (name of goddess, sacred metre)', 'गायत्री');
INSERT INTO "content"."declension_stems" VALUES ('35d66fe9-a3f9-49de-8c42-d70d35d98626', 'śṛṅga-', 'A_STEM', 'NEUTER', 'рог, вершина, пик', 'horn, peak, summit', 'शृङ्ग');
INSERT INTO "content"."declension_stems" VALUES ('c5ce3f72-f4e9-49e0-89fd-c737f2d99ebc', 'muhūrta-', 'A_STEM', 'MASCULINE', 'момент, мгновение; благоприятное время', 'moment, instant; auspicious time', 'मुहूर्त');
INSERT INTO "content"."declension_stems" VALUES ('f0509a8e-6bea-4497-9abb-2a063f885c87', 'artha-', 'A_STEM', 'MASCULINE', 'цель, польза, смысл, имущество', 'aim, benefit, meaning, wealth', 'अर्थ');
INSERT INTO "content"."declension_stems" VALUES ('1186346c-a83c-4827-b133-302efb68b1cb', 'hiraṇya-', 'A_STEM', 'NEUTER', 'золото, золотой', 'gold, golden', 'हिरण्य');
INSERT INTO "content"."declension_stems" VALUES ('03dba01b-7422-46b6-928a-024c510c5bc9', 'vikrama-', 'A_STEM', 'MASCULINE', 'героический подвиг, сила, доблесть', 'heroic deed, power, valor', 'विक्रम');
INSERT INTO "content"."declension_stems" VALUES ('ed5842d6-34bf-4962-b9f2-142377b8e17c', 'dhīra-', 'A_STEM', 'MASCULINE', 'мудрый, смелый, твёрдый', 'wise, brave, firm', 'धीर');
INSERT INTO "content"."declension_stems" VALUES ('f37e255d-edc2-4aa7-993c-bb155fd613b5', 'nirvāṇa-', 'A_STEM', 'NEUTER', 'нирвана, угасание, освобождение', 'nirvāṇa, extinction, liberation', 'निर्वाण');
INSERT INTO "content"."declension_stems" VALUES ('1c95b9ac-f3df-4803-a796-73ae4396bef3', 'pūrva-', 'A_STEM', 'MASCULINE', 'первый, передний, восточный', 'first, former, eastern', 'पूर्व');
INSERT INTO "content"."declension_stems" VALUES ('798c60fa-7347-4418-8bb8-876c9a9e03d1', 'vidyā-', 'AA_STEM', 'FEMININE', 'знание, наука, образование', 'knowledge, science, education', 'विद्या');
INSERT INTO "content"."declension_stems" VALUES ('6fec36ce-83b7-4086-8d7c-54388c479e64', 'rāśi-', 'II_STEM', 'FEMININE', 'куча, множество, груда', 'heap, multitude, mass', 'राशि');
INSERT INTO "content"."declension_stems" VALUES ('a036f5a1-d4fb-4f73-a397-06962b64f8e1', 'vajra-', 'A_STEM', 'MASCULINE', 'ваджра', 'vajra', 'वज्र');
INSERT INTO "content"."declension_stems" VALUES ('c98607bb-8bbf-486a-8bb1-09553e12c9fa', 'śāstra-', 'A_STEM', 'NEUTER', 'наука, трактат', 'science, treatise', 'शास्त्र');
INSERT INTO "content"."declension_stems" VALUES ('464d2be7-33bf-4568-97c1-0d3c8084283e', 'anila-', 'A_STEM', 'MASCULINE', 'ветер', 'wind', 'अनिल');
INSERT INTO "content"."declension_stems" VALUES ('f633cc65-520a-4fc2-b9fe-8c080a885d3c', 'sādhaka-', 'A_STEM', 'MASCULINE', 'достигающий, исполнитель', 'accomplisher', 'साधक');
INSERT INTO "content"."declension_stems" VALUES ('96dfbf29-3a2a-48b3-b4b4-83a3458d418a', 'kṣatra-', 'A_STEM', 'NEUTER', 'царская власть', 'royal power', 'क्षत्र');
INSERT INTO "content"."declension_stems" VALUES ('9477cb9f-4935-4c7c-9781-e7a8a78e767e', 'rājñī-', 'II_STEM', 'FEMININE', 'царица', 'queen', 'राज्ञी');
INSERT INTO "content"."declension_stems" VALUES ('6f98786d-e25d-4850-8499-6a33115a3ad8', 'bhūta-', 'A_STEM', 'MASCULINE', 'существо', 'being', 'भूत');
INSERT INTO "content"."declension_stems" VALUES ('2445eb13-56d4-49b7-af5a-af44ee33b32b', 'kṣīra-', 'A_STEM', 'NEUTER', 'молоко', 'milk', 'क्षीर');
INSERT INTO "content"."declension_stems" VALUES ('c31a4f54-9121-4762-9da2-7bd8fee31b74', 'dīkṣā-', 'AA_STEM', 'FEMININE', 'посвящение', 'initiation', 'दीक्षा');
INSERT INTO "content"."declension_stems" VALUES ('7f145afe-af23-4db5-a922-eb628ba28fe7', 'gārgī-', 'II_STEM', 'FEMININE', 'Гарги (имя)', 'Gargi (name)', 'गार्गी');
INSERT INTO "content"."declension_stems" VALUES ('f706a394-ae8c-4074-be79-0e535722e6ba', 'sarpa-', 'A_STEM', 'MASCULINE', 'змея', 'snake', 'सर्प');
INSERT INTO "content"."declension_stems" VALUES ('40addfc2-902b-4b31-a2bc-df365c871c3b', 'priya-', 'A_STEM', 'MASCULINE', 'дорогой, любимый', 'dear, beloved', 'प्रिय');
INSERT INTO "content"."declension_stems" VALUES ('304c835f-c6ea-48b6-a1bf-cabef420b8b8', 'vacana-', 'A_STEM', 'NEUTER', 'слово', 'word', 'वचन');
INSERT INTO "content"."declension_stems" VALUES ('7abe8d63-2b6c-40fa-b7b3-b82795e625f1', 'medha-', 'A_STEM', 'MASCULINE', 'жертвенное животное', 'sacrificial animal', 'मेध');
INSERT INTO "content"."declension_stems" VALUES ('ffcf20df-d05f-48f1-9d82-ae2821911eb3', 'vāda-', 'A_STEM', 'MASCULINE', 'речь, учение', 'speech, teaching', 'वाद');
INSERT INTO "content"."declension_stems" VALUES ('8aabdde8-e9dd-4336-b22a-64ccefd7f440', 'snāna-', 'A_STEM', 'NEUTER', 'омовение', 'bathing', 'स्नान');
INSERT INTO "content"."declension_stems" VALUES ('60121eec-27d4-4801-82bc-7c5cf777878c', 'gaura-', 'A_STEM', 'MASCULINE', 'белый, светлый', 'white, bright', 'गौर');
INSERT INTO "content"."declension_stems" VALUES ('51044de3-3b09-427d-bf55-a8fbb5916884', 'śabda-', 'A_STEM', 'MASCULINE', 'звук, слово', 'sound, word', 'शब्द');
INSERT INTO "content"."declension_stems" VALUES ('9fe4ddcf-dd78-43c6-a427-04701f9b8a52', 'rūpa-', 'A_STEM', 'NEUTER', 'форма, красота', 'form, beauty', 'रूप');
INSERT INTO "content"."declension_stems" VALUES ('a01e1fd0-4db9-44d2-ae28-c3e69fad2bf5', 'yama-', 'A_STEM', 'MASCULINE', 'Яма (бог смерти)', 'Yama (god of death)', 'यम');
INSERT INTO "content"."declension_stems" VALUES ('9842b824-f2e7-4053-a394-6a9b34cedb12', 'hasta-', 'A_STEM', 'MASCULINE', 'рука', 'hand', 'हस्त');
INSERT INTO "content"."declension_stems" VALUES ('f599f500-7352-4180-8ff6-d449cedc31d7', 'ānanda-', 'A_STEM', 'MASCULINE', 'блаженство', 'bliss', 'आनन्द');
INSERT INTO "content"."declension_stems" VALUES ('dd2d1df5-4bbc-4adb-bd94-32e6a933969a', 'tathāgata-', 'A_STEM', 'MASCULINE', 'Такой-пришедший (Будда)', 'Thus-Come (Buddha)', 'तथागत');
INSERT INTO "content"."declension_stems" VALUES ('cd7bdbcf-ba98-4a7c-b956-f54f1c0a481d', 'gandha-', 'A_STEM', 'MASCULINE', 'запах', 'smell', 'गन्ध');
INSERT INTO "content"."declension_stems" VALUES ('32cf48ff-9935-4c5c-bee6-4c6d392945b3', 'puṇya-', 'A_STEM', 'NEUTER', 'благо, заслуга', 'merit, virtue', 'पुण्य');
INSERT INTO "content"."declension_stems" VALUES ('73df2fd3-df09-41ce-bcfb-7934b0be3f48', 'tandrā-', 'AA_STEM', 'FEMININE', 'сонливость', 'drowsiness', 'तन्द्रा');
INSERT INTO "content"."declension_stems" VALUES ('c15f115d-8b5a-4b3c-9aa2-933e73823b46', 'rudhira-', 'A_STEM', 'NEUTER', 'кровь', 'blood', 'रुधिर');
INSERT INTO "content"."declension_stems" VALUES ('37ce9122-6381-4998-87e7-16d13b74c4e5', 'vṛkṣa-', 'A_STEM', 'MASCULINE', 'дерево', 'tree', 'वृक्ष');
INSERT INTO "content"."declension_stems" VALUES ('3feaf2fb-8135-4079-a3a9-08c70609bcca', 'bāla-', 'A_STEM', 'MASCULINE', 'ребёнок', 'child', 'बाल');
INSERT INTO "content"."declension_stems" VALUES ('1baf0d8c-6ad9-4bc7-bc43-7744ebaaddcc', 'bālā-', 'AA_STEM', 'FEMININE', 'девочка', 'girl', 'बाला');
INSERT INTO "content"."declension_stems" VALUES ('7d3e7860-5083-4777-92b9-937f836cb183', 'maṇḍala-', 'A_STEM', 'NEUTER', 'круг, окружность', 'circle', 'मण्डल');
INSERT INTO "content"."declension_stems" VALUES ('8c9ba603-bd50-42ed-af2c-50c6a99bfe98', 'aranyā-', 'AA_STEM', 'FEMININE', 'лес', 'forest', 'अरण्या');
INSERT INTO "content"."declension_stems" VALUES ('dfd1bb50-4ffd-4f77-b2db-7650ff57581a', 'loka-', 'A_STEM', 'MASCULINE', 'мир', 'world', 'लोक');
INSERT INTO "content"."declension_stems" VALUES ('82f27aa4-a588-4cfe-8cad-a00e24e066f6', 'argha-', 'A_STEM', 'MASCULINE', 'цена, значение', 'price, value', 'अर्घ');
INSERT INTO "content"."declension_stems" VALUES ('f74252cc-36e6-4b23-a253-ba945fcb662d', 'dharma-', 'A_STEM', 'MASCULINE', 'дхарма, закон', 'dharma, law', 'धर्म');
INSERT INTO "content"."declension_stems" VALUES ('0d4753f3-fd26-4b54-98dd-5e24398ab227', 'amṛta-', 'A_STEM', 'NEUTER', 'бессмертный; амрита (напиток бессмертия)', 'immortal; nectar', 'अमृत');
INSERT INTO "content"."declension_stems" VALUES ('5ae130fe-3a51-4769-93d7-a352da8ad0af', 'jīvita-', 'A_STEM', 'NEUTER', 'жизнь, существование', 'life, existence', 'जीवित');
INSERT INTO "content"."declension_stems" VALUES ('46c6a120-4c70-40bc-8e11-335fbeb4921e', 'nidrā-', 'AA_STEM', 'FEMININE', 'сон, сонливость', 'sleep, drowsiness', 'निद्रा');
INSERT INTO "content"."declension_stems" VALUES ('a37c5b86-9512-4e66-a21d-7d48fe2b14ef', 'veda-', 'A_STEM', 'MASCULINE', 'знание, веда, священное знание', 'knowledge, Veda, sacred knowledge', 'वेद');
INSERT INTO "content"."declension_stems" VALUES ('62a34096-5e20-4844-be83-ec7050a0390c', 'pāpa-', 'A_STEM', 'NEUTER', 'зло, грех, дурной поступок', 'evil, sin, bad deed', 'पाप');
INSERT INTO "content"."declension_stems" VALUES ('bcf509b4-447b-4014-a02a-955271220099', 'sarga-', 'A_STEM', 'MASCULINE', 'творение, создание, излияние', 'creation, emission, creation of the world', 'सर्ग');
INSERT INTO "content"."declension_stems" VALUES ('f2f76d7c-f733-4855-8f72-f22900bc1f5d', 'ādiparva-', 'A_STEM', 'NEUTER', 'первая книга (Махабхараты), книга о начале', 'first book (of Mahabharata); book of beginnings', 'आदिपर्व');
INSERT INTO "content"."declension_stems" VALUES ('7ba941a6-65db-42ce-aac0-0956496913c3', 'dhana-', 'A_STEM', 'NEUTER', 'богатство, деньги, имущество', 'wealth, money, property', 'धन');
INSERT INTO "content"."declension_stems" VALUES ('2b061430-0438-445e-aee1-05e9f1713d73', 'mata-', 'A_STEM', 'NEUTER', 'мысль, мнение, учение', 'thought, opinion, doctrine', 'मत');
INSERT INTO "content"."declension_stems" VALUES ('bd99b477-b115-407a-8c2a-c2fe65e18442', 'antarikṣa-', 'A_STEM', 'NEUTER', 'пространство, небо, атмосфера', 'space, sky, atmosphere', 'अन्तरिक्ष');
INSERT INTO "content"."declension_stems" VALUES ('32c9aced-826b-48a9-967a-fba2b096ace4', 'eka-', 'A_STEM', 'MASCULINE', 'один, единый; некий (в знач. местоим.)', 'one, sole; a certain one', 'एक');
INSERT INTO "content"."declension_stems" VALUES ('758d1d0c-20b8-48e8-86a0-5df727f05ebb', 'sama-', 'A_STEM', 'MASCULINE', 'равный, одинаковый, подобный', 'equal, same, similar', 'सम');
INSERT INTO "content"."declension_stems" VALUES ('902a5d4e-7ff4-4ff4-b910-146e47ff047a', 'nāga-', 'A_STEM', 'MASCULINE', 'змей, нага; слон (поэт.)', 'serpent, nāga; elephant (poet.)', 'नाग');
INSERT INTO "content"."declension_stems" VALUES ('50c4f18b-66d5-437b-b388-ac1185bc00e9', 'sara-', 'A_STEM', 'MASCULINE', 'текущий, двигающийся; озеро (м.р.)', 'flowing, moving; lake (masc.)', 'सर');
INSERT INTO "content"."declension_stems" VALUES ('d2ed1e77-e9c1-4ee2-9a18-ed1280444ab7', 'paṇḍita-', 'A_STEM', 'MASCULINE', 'учёный, мудрец, пандит', 'scholar, wise man, pandit', 'पण्डित');
INSERT INTO "content"."declension_stems" VALUES ('266b8a8e-c10f-4237-9586-eaad4112f4a5', 'yavana-', 'A_STEM', 'MASCULINE', 'грек, иониец; (позднее) иностранец', 'Greek, Ionian; (later) foreigner', 'यवन');
INSERT INTO "content"."declension_stems" VALUES ('ccc0fa59-fd9a-4871-9154-3490775e4e6f', 'āranyaka-', 'A_STEM', 'NEUTER', 'лесная книга, араньяка (часть Вед)', 'forest book, Aranyaka (Vedic text)', 'आरण्यक');
INSERT INTO "content"."declension_stems" VALUES ('61fae3e0-2c76-4c8c-80d4-57964eaa0e2b', 'gṛha-', 'A_STEM', 'NEUTER', 'дом, жилище (средний род)', 'house, dwelling (neuter)', 'गृह');
INSERT INTO "content"."declension_stems" VALUES ('25f0484f-b846-4dec-8c04-f0e160a81854', 'rasa-', 'A_STEM', 'MASCULINE', 'сок, вкус, сущность, чувство (эстетическое)', 'juice, taste, essence, sentiment (aesthetic)', 'रस');
INSERT INTO "content"."declension_stems" VALUES ('ad0857fb-c991-40bf-90f5-77e81343ade1', 'vipra-', 'A_STEM', 'MASCULINE', 'брахман, знаток Вед, мудрец', 'brahmin, Vedic scholar, sage', 'विप्र');
INSERT INTO "content"."declension_stems" VALUES ('9dc3622a-6a55-4770-879a-db4cf3d76b9d', 'kāma-', 'A_STEM', 'MASCULINE', 'желание, любовь, бог любви Кама', 'desire, love, god of love Kāma', 'काम');
INSERT INTO "content"."declension_stems" VALUES ('22b6152a-b609-4d9f-ac5c-c4f82580c138', 'grāma-', 'A_STEM', 'MASCULINE', 'деревня, община, толпа', 'village, community, crowd', 'ग्राम');
INSERT INTO "content"."declension_stems" VALUES ('b3f5b721-abda-453e-a533-a7ddfa07b44b', 'rājaputra-', 'A_STEM', 'MASCULINE', 'царевич, царский сын', 'prince, royal son', 'राजपुत्र');
INSERT INTO "content"."declension_stems" VALUES ('42a332e2-4918-40d9-970e-40a754e0295b', 'varṇa-', 'A_STEM', 'MASCULINE', 'цвет, цвет кожи, сословие, варна', 'color, complexion, caste, varṇa', 'वर्ण');
INSERT INTO "content"."declension_stems" VALUES ('a9af33b4-8fb7-4008-8f21-77b33387b856', 'brāhmaṇa-', 'A_STEM', 'MASCULINE', 'брахман, жрец, знаток Вед', 'brahmin, priest, Vedic scholar', 'ब्राह्मण');
INSERT INTO "content"."declension_stems" VALUES ('f037a75b-fea2-4beb-bd93-2c46b18d6b5b', 'indriya-', 'A_STEM', 'NEUTER', 'орган чувства, способность; сила', 'sense organ, faculty; power', 'इन्द्रिय');
INSERT INTO "content"."declension_stems" VALUES ('544c7db5-5ae9-4242-9b5e-43594ba5e009', 'dvāra-', 'A_STEM', 'NEUTER', 'дверь, врата; вход', 'door, gate; entrance', 'द्वार');
INSERT INTO "content"."declension_stems" VALUES ('4381ae30-0d84-4a1b-a4a5-02178811c40c', 'śūdra-', 'A_STEM', 'MASCULINE', 'шудра (представитель четвёртой варны)', 'Śūdra (member of the fourth varṇa)', 'शूद्र');
INSERT INTO "content"."declension_stems" VALUES ('1d24e761-e29c-4231-845f-73139f794418', 'bheṣaja-', 'A_STEM', 'NEUTER', 'лекарство, снадобье', 'medicine, drug, remedy', 'भेषज');
INSERT INTO "content"."declension_stems" VALUES ('4b8fadac-0f8f-4f59-b73d-9741ecbfb406', 'mati-', 'II_STEM', 'FEMININE', 'мысль, разум, мнение, намерение', 'thought, mind, opinion, intention', 'मति');
INSERT INTO "content"."declension_stems" VALUES ('247afa5d-4ed9-4800-99d2-daf93ab8ce5c', 'gati-', 'II_STEM', 'FEMININE', 'ход, движение, путь, состояние', 'motion, movement, path, state', 'गति');
INSERT INTO "content"."declension_stems" VALUES ('efc1a05e-06dd-42e0-945c-068d40dd6f4e', 'smṛti-', 'II_STEM', 'FEMININE', 'память, воспоминание; традиция', 'memory, remembrance; tradition', 'स्मृति');
INSERT INTO "content"."declension_stems" VALUES ('471332e5-8608-4257-b9a3-e142480ad157', 'bhūmi-', 'II_STEM', 'FEMININE', 'земля, почва, место', 'earth, ground, place', 'भूमि');
INSERT INTO "content"."declension_stems" VALUES ('b960d83b-a977-4147-afb3-b487b29959d4', 'śakti-', 'II_STEM', 'FEMININE', 'сила, энергия, способность', 'power, energy, ability', 'शक्ति');
INSERT INTO "content"."declension_stems" VALUES ('398c5c1f-7669-4667-9ca6-cf664351f414', 'vṛddhi-', 'II_STEM', 'FEMININE', 'рост, увеличение, процветание', 'growth, increase, prosperity', 'वृद्धि');
INSERT INTO "content"."declension_stems" VALUES ('a64d0ba3-c212-4847-a792-db97fa8b1b46', 'kīrti-', 'II_STEM', 'FEMININE', 'слава, известность, репутация', 'fame, renown, reputation', 'कीर्ति');
INSERT INTO "content"."declension_stems" VALUES ('7338a9e9-3aa8-4a4f-921c-5497c15aceaf', 'lakṣmī-', 'II_STEM', 'FEMININE', 'Лакшми (богиня удачи); красота, процветание', 'Lakshmi (goddess of fortune); beauty, prosperity', 'लक्ष्मी');
INSERT INTO "content"."declension_stems" VALUES ('0a21d70d-6ea4-453d-8e3d-74be27af8be3', 'pūrti-', 'II_STEM', 'FEMININE', 'завершение, исполнение, наполнение', 'completion, fulfilment, filling', 'पूर्ति');
INSERT INTO "content"."declension_stems" VALUES ('46cfbc79-c715-43d1-80d9-b187966015be', 'sṛṣṭi-', 'II_STEM', 'FEMININE', 'творение, создание, созидание', 'creation, creation, formation', 'सृष्टि');
INSERT INTO "content"."declension_stems" VALUES ('4cc022ba-f423-48c7-a348-cb7bd2242c1d', 'mukti-', 'II_STEM', 'FEMININE', 'освобождение, спасение, избавление', 'liberation, salvation, release', 'मुक्ति');
INSERT INTO "content"."declension_stems" VALUES ('b5b254e0-b184-44ae-a3fa-f1e345d639eb', 'dṛṣṭi-', 'II_STEM', 'FEMININE', 'взгляд, зрение, воззрение, мнение', 'sight, vision, viewpoint, opinion', 'दृष्टि');
INSERT INTO "content"."declension_stems" VALUES ('7ad37a6f-072e-468c-87ce-43510825f28a', 'ghṛṇi-', 'II_STEM', 'FEMININE', 'жара, зной, солнечный свет', 'heat, sunshine, ray of light', 'घृणि');
INSERT INTO "content"."declension_stems" VALUES ('9deb1c19-77ac-45ab-be08-1eaad88c0daf', 'kṣānti-', 'II_STEM', 'FEMININE', 'терпение, выносливость, прощение', 'patience, forbearance, forgiveness', 'क्षान्ति');
INSERT INTO "content"."declension_stems" VALUES ('6a5472b1-b21e-47a8-aedd-1346620c4556', 'bhakti-', 'II_STEM', 'FEMININE', 'преданность, любовь, почитание, бхакти', 'devotion, love, worship, bhakti', 'भक्ति');
INSERT INTO "content"."declension_stems" VALUES ('15c9c37f-784d-43cc-a8f2-776dd44e12b1', 'ṛddhi-', 'II_STEM', 'FEMININE', 'процветание, изобилие, благополучие; сиддхи', 'prosperity, abundance, well-being; siddhi', 'ऋद्धि');
INSERT INTO "content"."declension_stems" VALUES ('c70aea77-e40b-4e53-85ad-ca2398f9ad46', 'prīti-', 'II_STEM', 'FEMININE', 'удовольствие, радость, любовь, дружба', 'pleasure, joy, love, friendship', 'प्रीति');
INSERT INTO "content"."declension_stems" VALUES ('6a9e53c2-7474-465c-a909-3e3b2e23898b', 'dhṛti-', 'II_STEM', 'FEMININE', 'стойкость, смелость, постоянство, уверенность', 'steadiness, courage, constancy, confidence', 'धृति');
INSERT INTO "content"."declension_stems" VALUES ('d5feff69-87c1-43ca-bc97-b371437e2b3e', 'kavi-', 'I_STEM', 'MASCULINE', 'поэт, мудрец, провидец', 'poet, sage, seer', 'कवि');
INSERT INTO "content"."declension_stems" VALUES ('a2814187-4dfe-455a-9529-39cde006ecd3', 'muni-', 'I_STEM', 'MASCULINE', 'мудрец, отшельник, аскет', 'sage, hermit, ascetic', 'मुनि');
INSERT INTO "content"."declension_stems" VALUES ('d2807dfa-cffb-4928-9885-cdf2ee786b4f', 'ṛṣi-', 'I_STEM', 'MASCULINE', 'риши, ведийский мудрец', 'rishi, Vedic sage', 'ऋषि');
INSERT INTO "content"."declension_stems" VALUES ('117caff2-6858-45c6-96be-a25334333d23', 'hari-', 'I_STEM', 'MASCULINE', 'Хари (Вишну); жёлтый; лев', 'Hari (Vishnu); yellow; lion', 'हरि');
INSERT INTO "content"."declension_stems" VALUES ('5684f528-dcf3-44fe-b949-71a169a1d620', 'pāṇi-', 'I_STEM', 'MASCULINE', 'рука, кисть, длань', 'hand, palm', 'पाणि');
INSERT INTO "content"."declension_stems" VALUES ('a8f0a894-dc2f-408c-90b9-bb894639413d', 'nidhi-', 'I_STEM', 'MASCULINE', 'сокровище, клад; вместилище', 'treasure, hoard; receptacle', 'निधि');
INSERT INTO "content"."declension_stems" VALUES ('2afc38c7-9b51-4af4-aaea-facb7e08566d', 'ādi-', 'I_STEM', 'MASCULINE', 'начало, первопричина', 'beginning, first cause', 'आदि');
INSERT INTO "content"."declension_stems" VALUES ('7692d63b-afe1-4094-99e7-211e816ef2fd', 'praṇidhi-', 'I_STEM', 'MASCULINE', 'просьба, молитва; обет; шпион', 'request, prayer; vow; spy', 'प्रणिधि');
INSERT INTO "content"."declension_stems" VALUES ('47c92555-5258-4b8f-a4b1-389a78b417c8', 'bṛhaspati-', 'I_STEM', 'MASCULINE', 'Брихаспати (учитель богов, Юпитер)', 'Bṛhaspati (preceptor of gods, Jupiter)', 'बृहस्पति');
INSERT INTO "content"."declension_stems" VALUES ('7cf12ff8-938d-46b7-92dd-053fc46d973c', 'devī-', 'II_STEM', 'FEMININE', 'богиня', 'goddess', 'देवी');
INSERT INTO "content"."declension_stems" VALUES ('031ffdaf-4db9-4daa-bc81-50410a3ac958', 'sakhī-', 'II_STEM', 'FEMININE', 'подруга', 'female friend', 'सखी');
INSERT INTO "content"."declension_stems" VALUES ('61fe3212-22a0-40f4-9e81-721045b545c0', 'vadhū-', 'II_STEM', 'FEMININE', 'невеста, молодая жена', 'bride, young wife', 'वधू');
INSERT INTO "content"."declension_stems" VALUES ('784f0a6a-119f-4eb9-8769-fa05158ef423', 'śrī-', 'II_STEM', 'FEMININE', 'Шри (богиня); благополучие, красота', 'Śrī (goddess); prosperity, beauty', 'श्री');
INSERT INTO "content"."declension_stems" VALUES ('10d08c11-8a5d-46df-988c-1a1ca8c8a8c9', 'sarasvatī-', 'II_STEM', 'FEMININE', 'Сарасвати (богиня речи и знаний)', 'Sarasvatī (goddess of speech and knowledge)', 'सरस्वती');
INSERT INTO "content"."declension_stems" VALUES ('8c3a72fd-393e-4705-b337-24bfba2e6f4a', 'vāri-', 'I_STEM', 'NEUTER', 'вода', 'water', 'वारि');
INSERT INTO "content"."declension_stems" VALUES ('d395b023-8866-4aa1-bd19-448d2201a872', 'śari-', 'I_STEM', 'NEUTER', 'стрела (как оружие)', 'arrow (as a weapon)', 'शरि');
INSERT INTO "content"."declension_stems" VALUES ('8e78871f-211f-4d23-a287-e6e96cbab3a1', 'vāsi-', 'I_STEM', 'NEUTER', 'одежда, покрывало', 'garment, covering', 'वासि');
INSERT INTO "content"."declension_stems" VALUES ('dab5265c-5784-4c3b-9e04-b1f2908a24e6', 'akṣi-', 'I_STEM', 'NEUTER', 'глаз', 'eye', 'अक्षि');
INSERT INTO "content"."declension_stems" VALUES ('fb939ae6-d944-4ac7-a9af-480d4b230ac4', 'dāsi-', 'I_STEM', 'NEUTER', 'служение, рабство', 'servitude, slavery', 'दासि');
INSERT INTO "content"."declension_stems" VALUES ('5d5e39ea-8caf-4b00-9bc0-29597a3a8ed2', 'yoni-', 'I_STEM', 'NEUTER', 'лоно, источник; место рождения (ср. род)', 'womb, source; birthplace (neuter)', 'योनि');
INSERT INTO "content"."declension_stems" VALUES ('f24c4117-2052-4a72-aba4-7ae9db2b5696', 'ghṛta-', 'I_STEM', 'NEUTER', 'топлёное масло, гхи (в ср. роде)', 'clarified butter, ghee (neuter)', 'घृत');
INSERT INTO "content"."declension_stems" VALUES ('a91e2651-95ba-4d97-b07f-1fb93c906116', 'sānu-', 'I_STEM', 'NEUTER', 'вершина, гребень (в ср. роде)', 'peak, ridge (neuter)', 'सानु');
INSERT INTO "content"."declension_stems" VALUES ('ebf5db23-e6f1-487d-8103-2bb688253108', 'āsthā-', 'I_STEM', 'NEUTER', 'положение, состояние (в ср. роде)', 'position, condition (neuter)', 'आस्था');
INSERT INTO "content"."declension_stems" VALUES ('a41c7d48-1459-45f5-99e4-c603f56f322d', 'puru-', 'I_STEM', 'NEUTER', 'изобилие, множество (в ср. роде)', 'abundance, multitude (neuter)', 'पुरु');
INSERT INTO "content"."declension_stems" VALUES ('c22be9ae-8a06-494b-b290-51f4b1b6221d', 'śatru-', 'U_STEM', 'MASCULINE', 'враг, противник', 'enemy, adversary', 'शत्रु');
INSERT INTO "content"."declension_stems" VALUES ('fd251129-f8d1-4a2b-b942-02dbd0cdecf5', 'vāyu-', 'U_STEM', 'MASCULINE', 'ветер; бог ветра Ваю', 'wind; god of wind Vāyu', 'वायु');
INSERT INTO "content"."declension_stems" VALUES ('b3c6a539-9a1b-4433-a45d-559a6baa051a', 'ripu-', 'U_STEM', 'MASCULINE', 'враг, недруг', 'enemy, foe', 'रिपु');
INSERT INTO "content"."declension_stems" VALUES ('53f30949-d3c9-442a-b3e0-bb6d26b2f545', 'bandhu-', 'U_STEM', 'MASCULINE', 'родственник, друг, союзник', 'relative, friend, ally', 'बन्धु');
INSERT INTO "content"."declension_stems" VALUES ('af811d51-8965-4853-b2b0-5217d3528d2f', 'guru-', 'U_STEM', 'MASCULINE', 'учитель, наставник; уважаемый', 'teacher, preceptor; revered', 'गुरु');
INSERT INTO "content"."declension_stems" VALUES ('da3567cc-5771-4ada-bb26-3f51ba593dcd', 'sādhu-', 'U_STEM', 'MASCULINE', 'святой, отшельник; добродетельный', 'holy man, ascetic; virtuous', 'साधु');
INSERT INTO "content"."declension_stems" VALUES ('c7ced86e-2302-4677-828c-3fc160253c79', 'viṣṇu-', 'U_STEM', 'MASCULINE', 'Вишну (бог-хранитель)', 'Viṣṇu (preserver god)', 'विष्णु');
INSERT INTO "content"."declension_stems" VALUES ('a521130d-1f1e-4067-826f-e14f04695650', 'bhānu-', 'U_STEM', 'MASCULINE', 'солнце, луч света; сияние', 'sun, ray of light; radiance', 'भानु');
INSERT INTO "content"."declension_stems" VALUES ('76aa4a3e-6f09-4c10-b2e9-11bd49dd5774', 'prabhu-', 'U_STEM', 'MASCULINE', 'господин, владыка, правитель', 'master, lord, ruler', 'प्रभु');
INSERT INTO "content"."declension_stems" VALUES ('67ad6612-d01c-4a32-a0b6-92aaf521417c', 'sindhu-', 'U_STEM', 'MASCULINE', 'река (особ. Инд); море', 'river (esp. Indus); sea', 'सिन्धु');
INSERT INTO "content"."declension_stems" VALUES ('6453cdb0-e80d-40fe-895a-0109a1a620f3', 'kṛṣṇā-', 'U_STEM', 'MASCULINE', 'Кришна (в муж. роде)', 'Krishna (masc.)', 'कृष्ण');
INSERT INTO "content"."declension_stems" VALUES ('b4f7aa7e-1b08-4882-b9fe-fa7b7c9a0583', 'rāhu-', 'U_STEM', 'MASCULINE', 'Раху (восходящий узел Луны; демон)', 'Rahu (ascending lunar node; demon)', 'राहु');
INSERT INTO "content"."declension_stems" VALUES ('2bd2d7dc-38ad-4495-9474-4afb72fbcb58', 'ketu-', 'U_STEM', 'MASCULINE', 'Кету (нисходящий узел Луны; знамя)', 'Ketu (descending lunar node; banner)', 'केतु');
INSERT INTO "content"."declension_stems" VALUES ('56fcd82e-7a82-4323-9857-aed8bcd0cc15', 'mṛtyu-', 'U_STEM', 'MASCULINE', 'смерть', 'death', 'मृत्यु');
INSERT INTO "content"."declension_stems" VALUES ('d965ea94-1c75-4266-87ee-d5d0df3e0a5d', 'madhu-', 'U_STEM', 'MASCULINE', 'мёд; сладость', 'honey; sweetness', 'मधु');
INSERT INTO "content"."declension_stems" VALUES ('0b26212c-f72c-4546-a10c-937e08470cd5', 'tanu-', 'U_STEM', 'MASCULINE', 'тело (в муж. роде)', 'body (masc.)', 'तनु');
INSERT INTO "content"."declension_stems" VALUES ('16daab2b-4c39-4081-8045-d214294ec73f', 'dhanu-', 'U_STEM', 'MASCULINE', 'лук (оружие)', 'bow (weapon)', 'धनु');
INSERT INTO "content"."declension_stems" VALUES ('9444399e-33d8-4adb-abf6-b03a786be37d', 'sukha-', 'U_STEM', 'MASCULINE', 'счастье, радость (в муж. роде)', 'happiness, joy (masc.)', 'सुख');
INSERT INTO "content"."declension_stems" VALUES ('2e90f5a1-4ed6-40e2-85e7-db7f3cb5cf65', 'duḥkha-', 'U_STEM', 'MASCULINE', 'страдание, боль (в муж. роде)', 'suffering, pain (masc.)', 'दुःख');
INSERT INTO "content"."declension_stems" VALUES ('42433cee-2b70-4739-bf82-7ba4fbcc384d', 'mṛdū-', 'UU_STEM', 'FEMININE', 'мягкая, нежная (жен. род)', 'soft, gentle (feminine)', 'मृदू');
INSERT INTO "content"."declension_stems" VALUES ('58bd06df-485a-41cf-829c-62d265b805e9', 'śaśī-', 'UU_STEM', 'FEMININE', 'луна (в жен. роде)', 'moon (feminine)', 'शशी');
INSERT INTO "content"."declension_stems" VALUES ('52a82082-a56e-4285-9738-a6b67a0a6bf2', 'jāmbū-', 'UU_STEM', 'FEMININE', 'яблоня; Ямбу (мифический континент)', 'apple tree; Jambu (mythical continent)', 'जाम्बू');
INSERT INTO "content"."declension_stems" VALUES ('9e3241a4-8aab-42bd-abe0-33ad2ec44cbc', 'bhū-', 'UU_STEM', 'FEMININE', 'земля (в жен. роде)', 'earth (feminine)', 'भू');
INSERT INTO "content"."declension_stems" VALUES ('00b32696-3604-4408-8839-ffbfc0fded35', 'taru-', 'UU_STEM', 'FEMININE', 'дерево (в жен. роде, поэт.)', 'tree (feminine, poet.)', 'तरु');
INSERT INTO "content"."declension_stems" VALUES ('77e4a180-f2fd-4708-838a-da2dafa2986d', 'dhenu-', 'UU_STEM', 'FEMININE', 'корова (в жен. роде)', 'cow (feminine)', 'धेनु');
INSERT INTO "content"."declension_stems" VALUES ('9bf816ea-0e4a-438f-bbb1-9b2b82895aac', 'sūnu-', 'UU_STEM', 'FEMININE', 'дочь (в жен. роде)', 'daughter (feminine)', 'सूनु');
INSERT INTO "content"."declension_stems" VALUES ('eca5154e-0606-4286-a7fc-1874e1e4a3c9', 'pṛthu-', 'UU_STEM', 'FEMININE', 'широкая, большая (жен. род)', 'broad, large (feminine)', 'पृथू');
INSERT INTO "content"."declension_stems" VALUES ('c94aa63c-1c0b-4441-971e-b1943436075d', 'tanū-', 'UU_STEM', 'FEMININE', 'тело (в жен. роде)', 'body (feminine)', 'तनू');
INSERT INTO "content"."declension_stems" VALUES ('5135b6cc-577f-4980-95a3-bd6b537fd079', 'māsu-', 'U_STEM', 'NEUTER', 'месяц (ср. род)', 'month (neuter)', 'मासु');
INSERT INTO "content"."declension_stems" VALUES ('f7d3c678-423a-47bf-b8fa-074b6c65b37a', 'vasu-', 'U_STEM', 'NEUTER', 'богатство, добро, золото (ср. род)', 'wealth, good, gold (neuter)', 'वसु');
INSERT INTO "content"."declension_stems" VALUES ('27525ec2-e911-4efa-b901-44aac7432107', 'śatā-', 'U_STEM', 'NEUTER', 'сто (ср. род)', 'hundred (neuter)', 'शता');
INSERT INTO "content"."declension_stems" VALUES ('6db87a1a-a5be-4f03-b52f-c58763e08d4d', 'jānu-', 'U_STEM', 'NEUTER', 'колено (ср. род)', 'knee (neuter)', 'जानु');
INSERT INTO "content"."declension_stems" VALUES ('f022f985-597d-4406-a9f2-0946ccff1187', 'vadanā-', 'U_STEM', 'NEUTER', 'лицо (ср. род)', 'face (neuter)', 'वदना');
INSERT INTO "content"."declension_stems" VALUES ('67b0dcc8-e597-40fa-a863-a73884ebdbac', 'dāru-', 'U_STEM', 'NEUTER', 'дерево, древесина (ср. род)', 'wood, timber (neuter)', 'दारु');
INSERT INTO "content"."declension_stems" VALUES ('1112b75c-27d6-4ea9-a511-5f1b7c742ec9', 'bhāru-', 'U_STEM', 'NEUTER', 'бремя, груз (ср. род)', 'burden, load (neuter)', 'भारु');
INSERT INTO "content"."declension_stems" VALUES ('66b75867-104a-49fa-87b5-a9fce02dce4f', 'śālā-', 'U_STEM', 'NEUTER', 'зал, дом (в ср. роде)', 'hall, house (neuter)', 'शाला');
INSERT INTO "content"."declension_stems" VALUES ('ef4c3408-2a9b-4aff-8003-d372460657d5', 'paramā-', 'U_STEM', 'NEUTER', 'высочайший, превосходный (ср. род)', 'supreme, excellent (neuter)', 'परमा');
INSERT INTO "content"."declension_stems" VALUES ('35cd5ee8-fb16-4b90-ac27-79b2b661b560', 'sindhū-', 'II_STEM', 'FEMININE', 'река (жен. род)', 'river (feminine)', 'सिन्धू');
INSERT INTO "content"."declension_stems" VALUES ('b4d10d29-068c-4edd-8f8d-ef96961f52a4', 'vedanī-', 'II_STEM', 'FEMININE', 'боль, страдание; ощущение', 'pain, suffering; sensation', 'वेदनी');
INSERT INTO "content"."declension_stems" VALUES ('50d546ac-828f-440e-b532-ccc1c4206678', 'śramaṇī-', 'II_STEM', 'FEMININE', 'женщина-монах (буддийская монахиня)', 'female monk (Buddhist nun)', 'श्रमणी');
INSERT INTO "content"."declension_stems" VALUES ('6d16a956-ed0a-469f-bd25-259483b08dbf', 'brāhmaṇī-', 'II_STEM', 'FEMININE', 'женщина-брахман', 'brahmin woman', 'ब्राह्मणी');
INSERT INTO "content"."declension_stems" VALUES ('f80fca5c-b576-4b6c-a9ad-b76d15108200', 'kāraṇī-', 'II_STEM', 'FEMININE', 'причина, основание (жен. род)', 'cause, reason (feminine)', 'कारणी');
INSERT INTO "content"."declension_stems" VALUES ('7ac3f4a1-2164-472e-b49a-1b34f0431f45', 'kāminī-', 'II_STEM', 'FEMININE', 'женщина, возлюбленная', 'woman, beloved', 'कामिनी');
INSERT INTO "content"."declension_stems" VALUES ('da54b48d-e603-4845-93d1-034fa5ac1884', 'rogiṇī-', 'II_STEM', 'FEMININE', 'больная женщина', 'sick woman', 'रोगिणी');
INSERT INTO "content"."declension_stems" VALUES ('4898292b-1141-47db-b4a2-ddd6bd945a70', 'sūkṣmā-', 'II_STEM', 'FEMININE', 'тонкая, нежная (жен. род)', 'subtle, delicate (feminine)', 'सूक्ष्मा');
INSERT INTO "content"."declension_stems" VALUES ('c7d4e6f0-04ad-4aa9-bf46-fc95959d9e73', 'svarā-', 'II_STEM', 'FEMININE', 'звук (в жен. роде)', 'sound (feminine)', 'स्वरा');
INSERT INTO "content"."declension_stems" VALUES ('9cced209-8d3a-4bb3-b152-c7677744db27', 'sarasvā-', 'II_STEM', 'FEMININE', 'Сарасвати (богиня речи)', 'Sarasvatī (goddess of speech)', 'सरस्वा');
INSERT INTO "content"."declension_stems" VALUES ('967b949d-b7cd-418c-acac-dae7ea49df26', 'kālā-', 'AA_STEM', 'FEMININE', 'чёрная, тёмная', 'black, dark', 'काला');
INSERT INTO "content"."declension_stems" VALUES ('d3861969-2c40-4ac7-8f56-0af3da0d42d7', 'nīlā-', 'AA_STEM', 'FEMININE', 'синяя, тёмно-синяя', 'blue, dark-blue', 'नीला');
INSERT INTO "content"."declension_stems" VALUES ('9a74bc33-5db2-46f8-a1d3-5c9cfeb29737', 'piṅgalā-', 'AA_STEM', 'FEMININE', 'золотистая, рыжеватая', 'golden, reddish', 'पिङ्गला');
INSERT INTO "content"."declension_stems" VALUES ('7a722067-adb1-4cf7-9269-d5f2bccdefc0', 'raktā-', 'AA_STEM', 'FEMININE', 'красная', 'red', 'रक्ता');
INSERT INTO "content"."declension_stems" VALUES ('aed36c06-6f96-45e3-88bf-82006effdfc1', 'haritā-', 'AA_STEM', 'FEMININE', 'зелёная', 'green', 'हरिता');
INSERT INTO "content"."declension_stems" VALUES ('112c5f60-6527-4aee-bb79-362e57c857bd', 'śyāmā-', 'AA_STEM', 'FEMININE', 'тёмная, смуглая', 'dark, swarthy', 'श्यामा');
INSERT INTO "content"."declension_stems" VALUES ('745ed18d-3f05-4b1c-8f5d-6342dc527f43', 'pītā-', 'AA_STEM', 'FEMININE', 'жёлтая', 'yellow', 'पीता');
INSERT INTO "content"."declension_stems" VALUES ('e08d6369-3a37-4db6-b5e6-4dd0ef2af6c5', 'aruṇā-', 'AA_STEM', 'FEMININE', 'красноватая, багряная', 'reddish, crimson', 'अरुणा');
INSERT INTO "content"."declension_stems" VALUES ('980102c5-51cb-43a1-93fa-64672d429179', 'kapilā-', 'AA_STEM', 'FEMININE', 'коричневая, бурая', 'brown, tawny', 'कपिला');
INSERT INTO "content"."declension_stems" VALUES ('9d9a9c34-e30c-41ec-9a58-556ae7fda395', 'pṛthū-', 'UU_STEM', 'FEMININE', 'широкая, большая', 'broad, large', 'पृथू');
INSERT INTO "content"."declension_stems" VALUES ('359c1925-730a-400d-9400-a642e60be001', 'duhitṛ-', 'R_STEM', 'MASCULINE', 'дочь (в ведийском муж. род)', 'daughter (masc. in Vedic)', 'दुहितृ');
INSERT INTO "content"."declension_stems" VALUES ('6c657a5d-f072-4084-99c5-d15d5f07330e', 'naptṛ-', 'R_STEM', 'MASCULINE', 'внук', 'grandson', 'नप्तृ');
INSERT INTO "content"."declension_stems" VALUES ('484fa4c0-902b-488b-a541-7065300391d4', 'jāmātṛ-', 'R_STEM', 'MASCULINE', 'зять', 'son-in-law', 'जामातृ');
INSERT INTO "content"."declension_stems" VALUES ('f210f79d-1b32-41c5-a974-4becb26edd55', 'yātṛ-', 'R_STEM', 'MASCULINE', 'путник, путешественник', 'traveler, wanderer', 'यातृ');
INSERT INTO "content"."declension_stems" VALUES ('593be57f-1ea6-430f-bde1-4af073b71283', 'dātṛ-', 'R_STEM', 'MASCULINE', 'дающий, даритель', 'giver, donor', 'दातृ');
INSERT INTO "content"."declension_stems" VALUES ('d6710975-0722-4923-9a9c-fd61287088c6', 'kartṛ-', 'R_STEM', 'MASCULINE', 'делающий, деятель, творец', 'doer, agent, creator', 'कर्तृ');
INSERT INTO "content"."declension_stems" VALUES ('e2f8cc83-525a-482c-90ba-9c8493aeebde', 'netṛ-', 'R_STEM', 'MASCULINE', 'ведущий, вождь, правитель', 'leader, guide, ruler', 'नेतृ');
INSERT INTO "content"."declension_stems" VALUES ('b26e43cd-6693-4f13-85c1-756dee0512ac', 'savitṛ-', 'R_STEM', 'MASCULINE', 'Савитар (бог солнца); побудитель', 'Savitṛ (sun god); impeller', 'सवितृ');
INSERT INTO "content"."declension_stems" VALUES ('77dbf6cb-896a-4d68-8af3-720a4747be05', 'mātṛ-', 'R_STEM', 'FEMININE', 'мать', 'mother', 'मातृ');
INSERT INTO "content"."declension_stems" VALUES ('f50e5c09-4fd3-427f-8d35-b2990ce99e7c', 'svastṛ-', 'R_STEM', 'FEMININE', 'сестра', 'sister', 'स्वस्तृ');
INSERT INTO "content"."declension_stems" VALUES ('226f977c-ac86-4e93-a7bb-ffd642ae1d6e', 'nanāndṛ-', 'R_STEM', 'FEMININE', 'золовка (сестра мужа)', 'husband\''s sister', 'ननान्दृ');
INSERT INTO "content"."declension_stems" VALUES ('9c758222-e4ab-48c4-b10a-4a2754d65fb9', 'yātrā-', 'R_STEM', 'FEMININE', 'путешествие, поездка', 'journey, travel', 'यात्रा');
INSERT INTO "content"."declension_stems" VALUES ('62cdd39b-eb2b-4583-96f3-143e0fe0f23a', 'dātrī-', 'R_STEM', 'FEMININE', 'дающая, дарительница', 'giver (feminine)', 'दात्री');
INSERT INTO "content"."declension_stems" VALUES ('29ce36f6-adcd-41c4-a8b6-427c4eeee3d8', 'kartrī-', 'R_STEM', 'FEMININE', 'делающая, деятельница', 'doer (feminine)', 'कर्त्री');
INSERT INTO "content"."declension_stems" VALUES ('1727a9dd-47f9-4590-96dc-4e3ae8346016', 'naptrī-', 'R_STEM', 'FEMININE', 'внучка', 'granddaughter', 'नप्त्री');
INSERT INTO "content"."declension_stems" VALUES ('f2c91adf-7db7-41f5-af87-d1536de0b33f', 'netrī-', 'R_STEM', 'FEMININE', 'ведущая, вождиня', 'leader (feminine)', 'नेत्री');
INSERT INTO "content"."declension_stems" VALUES ('c9488de3-1277-4710-b6b7-24b5bd75d1b7', 'savitrī-', 'R_STEM', 'FEMININE', 'побудительница', 'impeller (feminine)', 'सवित्री');
INSERT INTO "content"."declension_stems" VALUES ('d1bc15ed-d59d-43ea-a66f-b49f2ab7df7d', 'śṛṅgi-', 'I_STEM', 'NEUTER', 'рогатое', 'horned', 'शृङ्गि');
INSERT INTO "content"."declension_stems" VALUES ('26959ffe-3c23-42e4-9cdb-435ed0c13343', 'kṣi-', 'I_STEM', 'NEUTER', 'жилище, обитель', 'dwelling, abode', 'क्षि');
INSERT INTO "content"."declension_stems" VALUES ('291b1cff-ee01-4a38-86e9-80a2ded1f1c6', 'anū-', 'U_STEM', 'NEUTER', 'позвоночник', 'spine', 'अनू');
INSERT INTO "content"."declension_stems" VALUES ('13b04557-d9ea-4640-8736-8cd2cfba6931', 'vyāghra-', 'A_STEM', 'MASCULINE', 'тигр', 'tiger', 'व्याघ्र');
INSERT INTO "content"."declension_stems" VALUES ('5c57d16e-b978-4ab2-a975-68ea8ebda240', 'śaṅkha-', 'A_STEM', 'MASCULINE', 'раковина; знак', 'shell; sign', 'शङ्ख');
INSERT INTO "content"."declension_stems" VALUES ('6c9aef65-d273-499e-ab24-e62b2654fa68', 'kumbha-', 'A_STEM', 'MASCULINE', 'кувшин; знак зодиака Водолей', 'pot; Aquarius', 'कुम्भ');
INSERT INTO "content"."declension_stems" VALUES ('41b1c819-0ac8-45a9-91b8-9bc68b5e9486', 'meṣa-', 'A_STEM', 'MASCULINE', 'баран; знак зодиака Овен', 'ram; Aries', 'मेष');
INSERT INTO "content"."declension_stems" VALUES ('4a357372-f860-4bde-a92a-d0c7f3ebfe4a', 'vṛṣabha-', 'A_STEM', 'MASCULINE', 'бык; знак зодиака Телец', 'bull; Taurus', 'वृषभ');
INSERT INTO "content"."declension_stems" VALUES ('65a4bfd7-2f60-4519-8f8c-6938ee9c4d34', 'āyudha-', 'A_STEM', 'NEUTER', 'оружие', 'weapon', 'आयुध');

-- ----------------------------
-- Uniques structure for table declension_stems
-- ----------------------------
ALTER TABLE "content"."declension_stems" ADD CONSTRAINT "declension_stems_stem_iast_key" UNIQUE ("stem_iast");

-- ----------------------------
-- Checks structure for table declension_stems
-- ----------------------------
ALTER TABLE "content"."declension_stems" ADD CONSTRAINT "ck_vowel_type" CHECK (vowel_type::text = ANY (ARRAY['A_STEM'::character varying::text, 'AA_STEM'::character varying::text, 'I_STEM'::character varying::text, 'II_STEM'::character varying::text, 'U_STEM'::character varying::text, 'UU_STEM'::character varying::text, 'R_STEM'::character varying::text]));
ALTER TABLE "content"."declension_stems" ADD CONSTRAINT "ck_gender" CHECK (gender::text = ANY (ARRAY['MASCULINE'::character varying::text, 'FEMININE'::character varying::text, 'NEUTER'::character varying::text, 'UNKNOWN'::character varying::text]));

-- ----------------------------
-- Primary Key structure for table declension_stems
-- ----------------------------
ALTER TABLE "content"."declension_stems" ADD CONSTRAINT "declension_stems_pkey" PRIMARY KEY ("id");
