package sm.selflearn.samskrtam.curriculum.questitem.controller;

public record RegenerateRequest(
        int lexemeLimit
) {
    public RegenerateRequest {
        if (lexemeLimit < 0) {
            lexemeLimit = 0;
        }
    }
}