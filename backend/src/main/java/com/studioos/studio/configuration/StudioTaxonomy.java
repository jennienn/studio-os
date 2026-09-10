package com.studioos.studio.configuration;

import com.studioos.common.ApiException;
import java.util.*;

public final class StudioTaxonomy {
    private StudioTaxonomy() {}
    public enum Category { LESSON, BEAUTY }
    public enum Capability { PRIVATE_LESSON, GROUP_CLASS, ATTENDANCE, CUSTOMER_BOOKING, PASS_MANAGEMENT, DEPOSIT, REVISIT }
    public record Branch(List<String> businessTypes, List<Capability> capabilities) {}
    private static final Map<Category, Branch> CATALOG = Map.of(
        Category.LESSON, new Branch(
            List.of("DANCE","PILATES","YOGA","PT","POLE","VOCAL_MUSIC","OTHER_LESSON"),
            List.of(Capability.PRIVATE_LESSON,Capability.GROUP_CLASS,Capability.ATTENDANCE,Capability.CUSTOMER_BOOKING,Capability.PASS_MANAGEMENT)),
        Category.BEAUTY, new Branch(
            List.of("NAIL","EYELASH","HAIR_EXTENSION","WAXING","HAIR","OTHER_BEAUTY"),
            List.of(Capability.CUSTOMER_BOOKING,Capability.DEPOSIT,Capability.REVISIT))
    );
    public static Map<Category,Branch> catalog() { return CATALOG; }
    public static Branch branch(Category category) { return CATALOG.get(category); }
    public static void validateType(Category category, String type) {
        if (category == null || !branch(category).businessTypes().contains(type))
            throw new ApiException(400,"INVALID_BUSINESS_TYPE","선택한 사업장 유형에 속한 세부 업종을 선택해 주세요.");
    }
}
