package client;

public class Review {
    private String customerUsername;
    private int rating;
    private String comments;

    public Review(String customerUsername, int rating, String comments) {
        this.customerUsername = customerUsername;
        this.rating = rating;
        this.comments = comments;
    }

    public String getCustomerUsername() {
        return customerUsername;
    }

    public int getRating() {
        return rating;
    }

    public String getComments() {
        return comments;
    }
}