package nl.utwente.ing.model;

public class CategoryRule {

    private Integer id;
    private String description;
    private String iBAN;
    private String type;
    private Category category;
    private Boolean applyOnHistory;

    public CategoryRule(Integer id, String description, String iBAN, String transactionType, Category category, Boolean applyOnHistory) {
        this.id = id;
        this.description = description;
        this.iBAN = iBAN;
        this.type = transactionType;
        this.category = category;
        this.applyOnHistory = applyOnHistory;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getiBAN() {
        return iBAN;
    }

    public void setiBAN(String iBAN) {
        this.iBAN = iBAN;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Boolean getApplyOnHistory() {
        return applyOnHistory;
    }

    public void setApplyOnHistory(Boolean applyOnHistory) {
        this.applyOnHistory = applyOnHistory;
    }
}
