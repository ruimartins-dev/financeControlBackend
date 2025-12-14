package com.example.financialcontrol.service;

import com.example.financialcontrol.dto.*;
import com.example.financialcontrol.entity.*;
import com.example.financialcontrol.repository.CategoryRepository;
import com.example.financialcontrol.repository.HiddenCategoryRepository;
import com.example.financialcontrol.repository.HiddenSubcategoryRepository;
import com.example.financialcontrol.repository.SubcategoryRepository;
import com.example.financialcontrol.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final SubcategoryRepository subcategoryRepository;
    private final HiddenCategoryRepository hiddenCategoryRepository;
    private final HiddenSubcategoryRepository hiddenSubcategoryRepository;
    private final TransactionRepository transactionRepository;

    public CategoryService(CategoryRepository categoryRepository,
                          SubcategoryRepository subcategoryRepository,
                          HiddenCategoryRepository hiddenCategoryRepository,
                          HiddenSubcategoryRepository hiddenSubcategoryRepository,
                          TransactionRepository transactionRepository) {
        this.categoryRepository = categoryRepository;
        this.subcategoryRepository = subcategoryRepository;
        this.hiddenCategoryRepository = hiddenCategoryRepository;
        this.hiddenSubcategoryRepository = hiddenSubcategoryRepository;
        this.transactionRepository = transactionRepository;
    }

    // ==================== CATEGORY OPERATIONS ====================

    /**
     * Get all categories available to a user (default + user's own), excluding hidden ones
     */
    public List<CategoryResponse> getAllCategoriesForUser(User user) {
        List<Category> categories = categoryRepository.findAllAvailableForUser(user.getId());
        Set<Long> hiddenCategoryIds = new HashSet<>(hiddenCategoryRepository.findHiddenCategoryIdsByUserId(user.getId()));
        Set<Long> hiddenSubcategoryIds = new HashSet<>(hiddenSubcategoryRepository.findHiddenSubcategoryIdsByUserId(user.getId()));

        return categories.stream()
                .filter(c -> !hiddenCategoryIds.contains(c.getId()))
                .map(c -> CategoryResponse.fromEntityWithFilteredSubcategories(c, user.getId(), hiddenSubcategoryIds))
                .collect(Collectors.toList());
    }

    /**
     * Get all categories available to a user by type (default + user's own), excluding hidden ones
     */
    public List<CategoryResponse> getCategoriesForUserByType(User user, TransactionType type) {
        List<Category> categories = categoryRepository.findAllAvailableForUserByType(user.getId(), type);
        Set<Long> hiddenCategoryIds = new HashSet<>(hiddenCategoryRepository.findHiddenCategoryIdsByUserId(user.getId()));
        Set<Long> hiddenSubcategoryIds = new HashSet<>(hiddenSubcategoryRepository.findHiddenSubcategoryIdsByUserId(user.getId()));

        return categories.stream()
                .filter(c -> !hiddenCategoryIds.contains(c.getId()))
                .map(c -> CategoryResponse.fromEntityWithFilteredSubcategories(c, user.getId(), hiddenSubcategoryIds))
                .collect(Collectors.toList());
    }

    /**
     * Get a single category by ID (only if accessible to user)
     */
    public CategoryResponse getCategoryById(Long categoryId, User user) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        // Check if user can access this category (either default or belongs to user)
        if (!category.isDefault() && (category.getUser() == null || !category.getUser().getId().equals(user.getId()))) {
            throw new RuntimeException("You don't have access to this category");
        }

        return CategoryResponse.fromEntityWithFilteredSubcategories(category, user.getId());
    }

    /**
     * Create a new category for a user
     */
    @Transactional
    public CategoryResponse createCategory(CategoryRequest request, User user) {
        // Check if category name already exists for user
        if (categoryRepository.existsByNameForUser(request.getName(), user.getId())) {
            throw new RuntimeException("A category with this name already exists");
        }

        Category category = new Category();
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        category.setColor(request.getColor());
        category.setIcon(request.getIcon());
        category.setType(request.getType());
        category.setDefault(false);
        category.setUser(user);

        category = categoryRepository.save(category);
        return CategoryResponse.fromEntity(category);
    }

    /**
     * Update a user's category (cannot update default categories)
     */
    @Transactional
    public CategoryResponse updateCategory(Long categoryId, CategoryRequest request, User user) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        // Check if it's a default category
        if (category.isDefault()) {
            throw new RuntimeException("Cannot modify default categories");
        }

        // Check if the category belongs to the user
        if (category.getUser() == null || !category.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("You don't have permission to modify this category");
        }

        category.setName(request.getName());
        category.setDescription(request.getDescription());
        category.setColor(request.getColor());
        category.setIcon(request.getIcon());
        category.setType(request.getType());

        category = categoryRepository.save(category);
        return CategoryResponse.fromEntityWithFilteredSubcategories(category, user.getId());
    }

    /**
     * Delete a user's category or hide a default category for this user
     */
    @Transactional
    public void deleteCategory(Long categoryId, User user) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        // If it's a default category, hide it for this user instead of deleting
        if (category.isDefault()) {
            if (!hiddenCategoryRepository.existsByUserIdAndCategoryId(user.getId(), categoryId)) {
                HiddenCategory hiddenCategory = new HiddenCategory(user, category);
                hiddenCategoryRepository.save(hiddenCategory);
            }
            return;
        }

        // Check if the category belongs to the user
        if (category.getUser() == null || !category.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("You don't have permission to delete this category");
        }

        // Check if there are transactions using this category
        if (transactionRepository.existsByCategoryNameAndUserId(category.getName(), user.getId())) {
            throw new RuntimeException("Cannot delete category: it is being used in one or more transactions");
        }

        categoryRepository.delete(category);
    }

    // ==================== SUBCATEGORY OPERATIONS ====================

    /**
     * Get all subcategories for a category (available to user)
     */
    public List<SubcategoryResponse> getSubcategoriesForCategory(Long categoryId, User user) {
        // First verify user has access to the category
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        if (!category.isDefault() && (category.getUser() == null || !category.getUser().getId().equals(user.getId()))) {
            throw new RuntimeException("You don't have access to this category");
        }

        List<Subcategory> subcategories = subcategoryRepository.findAllAvailableForUserByCategory(categoryId, user.getId());
        return subcategories.stream()
                .map(SubcategoryResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get a single subcategory by ID (only if accessible to user)
     */
    public SubcategoryResponse getSubcategoryById(Long subcategoryId, User user) {
        Subcategory subcategory = subcategoryRepository.findById(subcategoryId)
                .orElseThrow(() -> new RuntimeException("Subcategory not found"));

        // Check if user can access this subcategory
        if (!subcategory.isDefault() && (subcategory.getUser() == null || !subcategory.getUser().getId().equals(user.getId()))) {
            throw new RuntimeException("You don't have access to this subcategory");
        }

        return SubcategoryResponse.fromEntity(subcategory);
    }

    /**
     * Create a new subcategory for a user
     */
    @Transactional
    public SubcategoryResponse createSubcategory(SubcategoryRequest request, User user) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        // Check if user has access to this category
        if (!category.isDefault() && (category.getUser() == null || !category.getUser().getId().equals(user.getId()))) {
            throw new RuntimeException("You don't have access to this category");
        }

        // Check if subcategory name already exists for this category
        if (subcategoryRepository.existsByNameForCategoryAndUser(request.getName(), request.getCategoryId(), user.getId())) {
            throw new RuntimeException("A subcategory with this name already exists in this category");
        }

        Subcategory subcategory = new Subcategory();
        subcategory.setName(request.getName());
        subcategory.setDescription(request.getDescription());
        subcategory.setColor(request.getColor());
        subcategory.setIcon(request.getIcon());
        subcategory.setCategory(category);
        subcategory.setDefault(false);
        subcategory.setUser(user);

        subcategory = subcategoryRepository.save(subcategory);
        return SubcategoryResponse.fromEntity(subcategory);
    }

    /**
     * Update a user's subcategory (cannot update default subcategories)
     */
    @Transactional
    public SubcategoryResponse updateSubcategory(Long subcategoryId, SubcategoryRequest request, User user) {
        Subcategory subcategory = subcategoryRepository.findById(subcategoryId)
                .orElseThrow(() -> new RuntimeException("Subcategory not found"));

        // Check if it's a default subcategory
        if (subcategory.isDefault()) {
            throw new RuntimeException("Cannot modify default subcategories");
        }

        // Check if the subcategory belongs to the user
        if (subcategory.getUser() == null || !subcategory.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("You don't have permission to modify this subcategory");
        }

        // If changing category, verify access
        if (!subcategory.getCategory().getId().equals(request.getCategoryId())) {
            Category newCategory = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found"));

            if (!newCategory.isDefault() && (newCategory.getUser() == null || !newCategory.getUser().getId().equals(user.getId()))) {
                throw new RuntimeException("You don't have access to the target category");
            }
            subcategory.setCategory(newCategory);
        }

        subcategory.setName(request.getName());
        subcategory.setDescription(request.getDescription());
        subcategory.setColor(request.getColor());
        subcategory.setIcon(request.getIcon());

        subcategory = subcategoryRepository.save(subcategory);
        return SubcategoryResponse.fromEntity(subcategory);
    }

    /**
     * Delete a user's subcategory or hide a default subcategory for this user
     */
    @Transactional
    public void deleteSubcategory(Long subcategoryId, User user) {
        Subcategory subcategory = subcategoryRepository.findById(subcategoryId)
                .orElseThrow(() -> new RuntimeException("Subcategory not found"));

        // If it's a default subcategory, hide it for this user instead of deleting
        if (subcategory.isDefault()) {
            if (!hiddenSubcategoryRepository.existsByUserIdAndSubcategoryId(user.getId(), subcategoryId)) {
                HiddenSubcategory hiddenSubcategory = new HiddenSubcategory(user, subcategory);
                hiddenSubcategoryRepository.save(hiddenSubcategory);
            }
            return;
        }

        // Check if the subcategory belongs to the user
        if (subcategory.getUser() == null || !subcategory.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("You don't have permission to delete this subcategory");
        }

        // Check if there are transactions using this subcategory
        if (transactionRepository.existsBySubcategoryNameAndUserId(subcategory.getName(), user.getId())) {
            throw new RuntimeException("Cannot delete subcategory: it is being used in one or more transactions");
        }

        subcategoryRepository.delete(subcategory);
    }

    // ==================== RESTORE HIDDEN OPERATIONS ====================

    /**
     * Get all hidden categories for a user
     */
    public List<CategoryResponse> getHiddenCategoriesForUser(User user) {
        List<Long> hiddenCategoryIds = hiddenCategoryRepository.findHiddenCategoryIdsByUserId(user.getId());
        return hiddenCategoryIds.stream()
                .map(id -> categoryRepository.findById(id).orElse(null))
                .filter(c -> c != null)
                .map(CategoryResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get all hidden subcategories for a user
     */
    public List<SubcategoryResponse> getHiddenSubcategoriesForUser(User user) {
        List<Long> hiddenSubcategoryIds = hiddenSubcategoryRepository.findHiddenSubcategoryIdsByUserId(user.getId());
        return hiddenSubcategoryIds.stream()
                .map(id -> subcategoryRepository.findById(id).orElse(null))
                .filter(s -> s != null)
                .map(SubcategoryResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Restore a hidden category for a user
     */
    @Transactional
    public void restoreCategory(Long categoryId, User user) {
        if (!hiddenCategoryRepository.existsByUserIdAndCategoryId(user.getId(), categoryId)) {
            throw new RuntimeException("Category is not hidden");
        }
        hiddenCategoryRepository.deleteByUserIdAndCategoryId(user.getId(), categoryId);
    }

    /**
     * Restore a hidden subcategory for a user
     */
    @Transactional
    public void restoreSubcategory(Long subcategoryId, User user) {
        if (!hiddenSubcategoryRepository.existsByUserIdAndSubcategoryId(user.getId(), subcategoryId)) {
            throw new RuntimeException("Subcategory is not hidden");
        }
        hiddenSubcategoryRepository.deleteByUserIdAndSubcategoryId(user.getId(), subcategoryId);
    }

    // ==================== DEFAULT DATA INITIALIZATION ====================

    /**
     * Initialize default categories and subcategories if they don't exist
     * This should be called on application startup
     */
    @Transactional
    public void initializeDefaultCategories() {
        // Check if default categories already exist
        if (!categoryRepository.findByIsDefaultTrue().isEmpty()) {
            return;
        }

        // DEBIT (Expense) Categories
        createDefaultCategoryWithSubcategories("Food & Dining", TransactionType.DEBIT, "#FF6B6B",
                new String[]{"Restaurants", "Groceries", "Fast Food", "Coffee", "Delivery"});

        createDefaultCategoryWithSubcategories("Transportation", TransactionType.DEBIT, "#4ECDC4",
                new String[]{"Fuel", "Public Transport", "Taxi/Uber", "Parking", "Car Maintenance"});

        createDefaultCategoryWithSubcategories("Shopping", TransactionType.DEBIT, "#45B7D1",
                new String[]{"Clothing", "Electronics", "Home & Garden", "Personal Care", "Gifts"});

        createDefaultCategoryWithSubcategories("Bills & Utilities", TransactionType.DEBIT, "#96CEB4",
                new String[]{"Electricity", "Water", "Internet", "Phone", "Rent/Mortgage"});

        createDefaultCategoryWithSubcategories("Entertainment", TransactionType.DEBIT, "#FFEAA7",
                new String[]{"Movies", "Games", "Concerts", "Sports", "Subscriptions"});

        createDefaultCategoryWithSubcategories("Health", TransactionType.DEBIT, "#DDA0DD",
                new String[]{"Medical", "Pharmacy", "Gym", "Insurance"});

        createDefaultCategoryWithSubcategories("Education", TransactionType.DEBIT, "#98D8C8",
                new String[]{"Courses", "Books", "School Supplies", "Tuition"});

        createDefaultCategoryWithSubcategories("Other Expenses", TransactionType.DEBIT, "#C0C0C0",
                new String[]{"Miscellaneous", "Fees", "Donations"});

        // CREDIT (Income) Categories
        createDefaultCategoryWithSubcategories("Salary", TransactionType.CREDIT, "#2ECC71",
                new String[]{"Monthly Salary", "Bonus", "Overtime", "Commission"});

        createDefaultCategoryWithSubcategories("Investments", TransactionType.CREDIT, "#3498DB",
                new String[]{"Dividends", "Interest", "Capital Gains", "Rental Income"});

        createDefaultCategoryWithSubcategories("Freelance", TransactionType.CREDIT, "#9B59B6",
                new String[]{"Consulting", "Projects", "Gigs"});

        createDefaultCategoryWithSubcategories("Gifts Received", TransactionType.CREDIT, "#E74C3C",
                new String[]{"Birthday", "Holiday", "Other"});

        createDefaultCategoryWithSubcategories("Other Income", TransactionType.CREDIT, "#95A5A6",
                new String[]{"Refunds", "Cashback", "Reimbursements", "Miscellaneous"});
    }

    private void createDefaultCategoryWithSubcategories(String categoryName, TransactionType type, String color, String[] subcategoryNames) {
        Category category = new Category();
        category.setName(categoryName);
        category.setType(type);
        category.setColor(color);
        category.setDefault(true);
        category.setUser(null);
        category = categoryRepository.save(category);

        // Always add "Geral" (General) subcategory first
        Subcategory geralSubcategory = new Subcategory();
        geralSubcategory.setName("General");
        geralSubcategory.setCategory(category);
        geralSubcategory.setDefault(true);
        geralSubcategory.setUser(null);
        subcategoryRepository.save(geralSubcategory);

        for (String subcategoryName : subcategoryNames) {
            Subcategory subcategory = new Subcategory();
            subcategory.setName(subcategoryName);
            subcategory.setCategory(category);
            subcategory.setDefault(true);
            subcategory.setUser(null);
            subcategoryRepository.save(subcategory);
        }
    }
}

