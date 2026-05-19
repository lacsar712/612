package com.lianzhi.wms.repository;

import com.lianzhi.wms.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {}
