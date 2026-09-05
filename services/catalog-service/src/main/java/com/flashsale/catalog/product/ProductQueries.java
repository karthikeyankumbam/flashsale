package com.flashsale.catalog.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Collation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.TreeSet;
import java.util.regex.Pattern;

@Repository
public class ProductQueries {
    private final MongoTemplate mongo;

    public ProductQueries(MongoTemplate mongo) { this.mongo = mongo; }

    public Page<Product> search(ProductSearch search, Boolean active) {
        var pageable = search.pageable();
        Query query = new Query();
        if (active != null) query.addCriteria(Criteria.where("active").is(active));
        if (search.category() != null) query.addCriteria(Criteria.where("category").regex(exact(search.category())));
        if (search.query() != null) {
            Pattern literal = Pattern.compile(Pattern.quote(search.query()), Pattern.CASE_INSENSITIVE);
            query.addCriteria(new Criteria().orOperator(
                    Criteria.where("name").regex(literal), Criteria.where("sku").regex(literal)));
        }
        if (search.currency() != null) query.addCriteria(Criteria.where("currency").regex(exact(search.currency())));
        if (search.minPrice() != null || search.maxPrice() != null) {
            Criteria price = Criteria.where("price");
            if (search.minPrice() != null) price.gte(search.minPrice());
            if (search.maxPrice() != null) price.lte(search.maxPrice());
            query.addCriteria(price);
        }
        query.collation(Collation.of("en").strength(Collation.ComparisonLevel.secondary()).caseLevel(true));
        long total = mongo.count(query, Product.class);
        return new PageImpl<>(mongo.find(query.with(pageable), Product.class), pageable, total);
    }

    public List<String> categories() {
        List<String> values = mongo.findDistinct(Query.query(Criteria.where("active").is(true)),
                "category", Product.class, String.class);
        TreeSet<String> categories = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        values.stream().filter(value -> value != null && !value.isBlank())
                .map(String::trim).sorted().forEach(categories::add);
        return List.copyOf(categories);
    }

    public Product update(String sku, Update changes) {
        Product product = mongo.findAndModify(Query.query(Criteria.where("sku").is(sku)), changes,
                FindAndModifyOptions.options().returnNew(true), Product.class);
        if (product == null) throw new ProductNotFoundException(sku);
        return product;
    }

    private Pattern exact(String value) {
        return Pattern.compile("^" + Pattern.quote(value) + "$", Pattern.CASE_INSENSITIVE);
    }
}
