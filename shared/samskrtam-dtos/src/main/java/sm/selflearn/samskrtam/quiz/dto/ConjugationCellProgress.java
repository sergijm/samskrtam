package sm.selflearn.samskrtam.quiz.dto;

/**
 * One cell of the conjugation progress grid: voice × person × number.
 */
public class ConjugationCellProgress {

    private String voice;
    private int person;
    private String numberType;
    private int score;
    private String status;

    public ConjugationCellProgress() {}

    public ConjugationCellProgress(String voice, int person, String numberType, int score, String status) {
        this.voice = voice;
        this.person = person;
        this.numberType = numberType;
        this.score = score;
        this.status = status;
    }

    public String getVoice() { return voice; }
    public void setVoice(String voice) { this.voice = voice; }

    public int getPerson() { return person; }
    public void setPerson(int person) { this.person = person; }

    public String getNumberType() { return numberType; }
    public void setNumberType(String numberType) { this.numberType = numberType; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}