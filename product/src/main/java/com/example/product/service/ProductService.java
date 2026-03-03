package com.example.product.service;

import com.example.product.model.Product;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class ProductService {

    public List<Product> getAllProducts() {

        return Arrays.asList(
                new Product("1", 2342, "Laptop", "Anything", 5),
                new Product("2", 2352, "Umbrella", "Anything", 5),
                new Product("3", 2362, "CPUs", "Anything", 5)
        );
    }
}