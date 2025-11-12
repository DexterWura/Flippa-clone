package com.flippa.service;

import com.flippa.entity.Category;
import com.flippa.repository.CategoryRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CategoryService {
    
    private static final Logger logger = LoggerFactory.getLogger(CategoryService.class);
    private final CategoryRepository categoryRepository;
    private final AuditLogService auditLogService;
    
    public CategoryService(CategoryRepository categoryRepository, AuditLogService auditLogService) {
        this.categoryRepository = categoryRepository;
        this.auditLogService = auditLogService;
    }
    
    public List<Category> getAllEnabledCategories() {
        return categoryRepository.findByEnabledTrueOrderByDisplayOrderAsc();
    }
    
    public List<Category> getAllCategories() {
        return categoryRepository.findAllByOrderByDisplayOrderAsc();
    }
    
    public Optional<Category> findById(Long id) {
        return categoryRepository.findById(id);
    }
    
    public Optional<Category> findByName(String name) {
        return categoryRepository.findByName(name);
    }
    
    @Transactional
    public Category createCategory(String name, String description, Integer displayOrder, 
                                   com.flippa.entity.User adminUser, HttpServletRequest request) {
        if (categoryRepository.findByName(name).isPresent()) {
            throw new RuntimeException("Category with name '" + name + "' already exists");
        }
        
        Category category = new Category();
        category.setName(name);
        category.setDescription(description);
        category.setDisplayOrder(displayOrder != null ? displayOrder : 0);
        category.setEnabled(true);
        
        Category savedCategory = categoryRepository.save(category);
        
        auditLogService.logAction(adminUser, "CATEGORY_CREATED", "Category", 
                                 savedCategory.getId().toString(), 
                                 "Category created: " + name, request);
        
        logger.info("Category created: {} by admin: {}", name, adminUser.getEmail());
        return savedCategory;
    }
    
    @Transactional
    public Category updateCategory(Long id, String name, String description, Integer displayOrder, 
                                   Boolean enabled, com.flippa.entity.User adminUser, HttpServletRequest request) {
        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Category not found"));
        
        // Check if name is being changed and if new name already exists
        if (!category.getName().equals(name) && categoryRepository.findByName(name).isPresent()) {
            throw new RuntimeException("Category with name '" + name + "' already exists");
        }
        
        category.setName(name);
        if (description != null) {
            category.setDescription(description);
        }
        if (displayOrder != null) {
            category.setDisplayOrder(displayOrder);
        }
        if (enabled != null) {
            category.setEnabled(enabled);
        }
        
        Category updatedCategory = categoryRepository.save(category);
        
        auditLogService.logAction(adminUser, "CATEGORY_UPDATED", "Category", 
                                 id.toString(), 
                                 "Category updated: " + name, request);
        
        logger.info("Category updated: {} by admin: {}", name, adminUser.getEmail());
        return updatedCategory;
    }
    
    @Transactional
    public void deleteCategory(Long id, com.flippa.entity.User adminUser, HttpServletRequest request) {
        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Category not found"));
        
        // Check if category has listings
        if (!category.getListings().isEmpty()) {
            throw new RuntimeException("Cannot delete category with existing listings");
        }
        
        categoryRepository.delete(category);
        
        auditLogService.logAction(adminUser, "CATEGORY_DELETED", "Category", 
                                 id.toString(), 
                                 "Category deleted: " + category.getName(), request);
        
        logger.info("Category deleted: {} by admin: {}", category.getName(), adminUser.getEmail());
    }
}

