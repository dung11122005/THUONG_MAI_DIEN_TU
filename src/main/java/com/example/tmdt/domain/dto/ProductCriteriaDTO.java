package com.example.tmdt.domain.dto;

import java.util.List;
import java.util.Optional;

public class ProductCriteriaDTO {
    private Optional<String> page;
    private Optional<List<String>> factory;
    private Optional<List<String>> target;
    private Optional<List<String>> price;
    private Optional<String> sort;
    private Optional<String> valueStar;
    private Optional<String> searchValue;
    private Optional<String> shipSearchValue;

    public Optional<String> getPage() {
        return page;
    }

    public void setPage(Optional<String> page) {
        this.page = page;
    }

    public Optional<List<String>> getFactory() {
        return factory;
    }

    public void setFactory(Optional<List<String>> factory) {
        this.factory = factory;
    }

    public Optional<List<String>> getTarget() {
        return target;
    }

    public void setTarget(Optional<List<String>> target) {
        this.target = target;
    }

    public Optional<List<String>> getPrice() {
        return price;
    }

    public void setPrice(Optional<List<String>> price) {
        this.price = price;
    }

    public Optional<String> getSort() {
        return sort;
    }

    public void setSort(Optional<String> sort) {
        this.sort = sort;
    }

    public Optional<String> getValueStar() {
        return valueStar;
    }

    public void setValueStar(Optional<String> valueStar) {
        this.valueStar = valueStar;
    }

    public Optional<String> getSearchValue() {
        return searchValue;
    }

    public void setSearchValue(Optional<String> searchValue) {
        this.searchValue = searchValue;
    }

    public Optional<String> getShipSearchValue() {
        return shipSearchValue;
    }

    public void setShipSearchValue(Optional<String> shipSearchValue) {
        this.shipSearchValue = shipSearchValue;
    }

}
