package com.example.testproject.converter;

import com.example.testproject.entity.Status;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class StatusConverter implements AttributeConverter<Status, Short> {
    @Override
    public Short convertToDatabaseColumn(Status status) {
        return status.getCode();
    }

    @Override
    public Status convertToEntityAttribute(Short code) {
        return Status.getByCode(code);
    }
}
