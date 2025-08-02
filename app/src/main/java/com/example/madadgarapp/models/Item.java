package com.example.madadgarapp.models;

/**
 * Model class representing an item that can be shared or viewed in the app.
 */
public class Item implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private String id;
    private String title;
    private String description;
    private String mainCategory;
    private String subCategory;
    private String location;
    // Geographic coordinates (nullable for legacy items)
    private Double latitude;
    private Double longitude;
    private String contactNumber;
    private String ownerEmail;
    private String imageUrl;
    private java.util.List<String> imageUrls;
    private String videoUrl;
    private String ownerId;
    private long createdAt;
    private long expiryTime;

    // Number of times this item has been viewed – used for "POPULAR" badge
    private int viewCount;

    // Empty constructor for Firebase
    public Item() {
    }

    // Constructor with coordinates
    public Item(String id, String title, String description, String mainCategory, 
                String subCategory, String location, Double latitude, Double longitude,
                String contactNumber, String ownerEmail, String imageUrl, String ownerId, long createdAt, long expiryTime) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.mainCategory = mainCategory;
        this.subCategory = subCategory;
        this.location = location;
        this.latitude = latitude;
        this.longitude = longitude;
        this.contactNumber = contactNumber;
        this.ownerEmail = ownerEmail;
        this.imageUrl = imageUrl;
        this.ownerId = ownerId;
        this.createdAt = createdAt;
        this.expiryTime = expiryTime;
    }

    // Legacy constructor without coordinates for backward compatibility
    public Item(String id, String title, String description, String mainCategory, 
                String subCategory, String location, String contactNumber,
                String imageUrl, String ownerId, long createdAt, long expiryTime) {
        this(id, title, description, mainCategory, subCategory, location, null, null,
             contactNumber, null, imageUrl, ownerId, createdAt, expiryTime);
    }

    // Legacy constructor that already included ownerEmail (still kept for calls that supply it)
    // Legacy constructor without coordinates for backward compatibility
    public Item(String id, String title, String description, String mainCategory, 
                String subCategory, String location, String contactNumber, String ownerEmail,
                String imageUrl, String ownerId, long createdAt, long expiryTime) {
        this(id, title, description, mainCategory, subCategory, location, null, null,
             contactNumber, ownerEmail, imageUrl, ownerId, createdAt, expiryTime);
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getMainCategory() {
        return mainCategory;
    }

    public void setMainCategory(String mainCategory) {
        this.mainCategory = mainCategory;
    }

    public String getSubCategory() {
        return subCategory;
    }

    public void setSubCategory(String subCategory) {
        this.subCategory = subCategory;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getOwnerEmail() {
        return ownerEmail;
    }

    public void setOwnerEmail(String ownerEmail) {
        this.ownerEmail = ownerEmail;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public java.util.List<String> getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(java.util.List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getExpiryTime() {
        return expiryTime;
    }

    public void setExpiryTime(long expiryTime) {
        this.expiryTime = expiryTime;
    }

    /**
     * Get full category string in format "MainCategory > SubCategory"
     */
    public String getFullCategory() {
        return mainCategory + " > " + subCategory;
    }

    /**
     * Check if this item matches the given search query and category
     */
    public boolean matchesFilters(String query, String category) {
        boolean matchesQuery = query == null || query.isEmpty() || 
                               title.toLowerCase().contains(query.toLowerCase()) || 
                               description.toLowerCase().contains(query.toLowerCase());
        
        boolean matchesCategory = category == null || category.isEmpty() || 
                                 getFullCategory().equals(category);
        
        return matchesQuery && matchesCategory;
    }

    public String getOwner() {
        return ownerId;
    }

    public String getContact() {
        return contactNumber;
    }

    public long getTimestamp() {
        return createdAt;
    }

    public int getViewCount() {
        return viewCount;
    }

    public void setViewCount(int viewCount) {
        this.viewCount = viewCount;
    }

    /**
     * Compatibility getter for older code – same as {@link #getViewCount()}.
     */
    public int viewCount() {
        return viewCount;
    }

    public long getExpiration() {
        return expiryTime;
    }

    public String getCategory() {
        return mainCategory + " > " + subCategory;
    }

}
