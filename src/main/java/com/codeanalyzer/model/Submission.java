package com.codeanalyzer.model;

public class Submission {
    private String submissionId;
    private String contestId;
    private String handle;
    private String sourceCode;
    private String aiAnalysis;

    public Submission(String submissionId, String contestId, String handle, String sourceCode, String aiAnalysis) {
        this.submissionId = submissionId;
        this.contestId = contestId;
        this.handle = handle;
        this.sourceCode = sourceCode;
        this.aiAnalysis = aiAnalysis;
    }

    // Getters and Setters
    public String getSubmissionId() { return submissionId; }
    public void setSubmissionId(String submissionId) { this.submissionId = submissionId; }
    public String getContestId() { return contestId; }
    public void setContestId(String contestId) { this.contestId = contestId; }
    public String getHandle() { return handle; }
    public void setHandle(String handle) { this.handle = handle; }
    public String getSourceCode() { return sourceCode; }
    public void setSourceCode(String sourceCode) { this.sourceCode = sourceCode; }
    public String getAiAnalysis() { return aiAnalysis; }
    public void setAiAnalysis(String aiAnalysis) { this.aiAnalysis = aiAnalysis; }
}
