package com.apollocurrency.aplwallet.apl.core.rest.parameter;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class RestParameterValidator implements ConstraintValidator<Validate, RestParameter<?>> {
   private boolean required;
   private String paramName;
   private String defaultValue;

   public void initialize(Validate constraint) {
      this.required = constraint.required();
      this.paramName = constraint.name();
      this.defaultValue = constraint.defaultValue();
   }

   public boolean isValid(RestParameter<?> parameter, ConstraintValidatorContext context) {
      context.disableDefaultConstraintViolation();
      if (parameter == null && !required) return true;

      if (parameter != null) {
         try {
            Object value = parameter.get();
            return true;
         } catch (Exception ignored) {
         }
      }

      String messageTemplate = context.getDefaultConstraintMessageTemplate();
      if (messageTemplate == null || messageTemplate.isEmpty()) {
         messageTemplate = "Invalid REST parameter "+paramName;
      }
      context.buildConstraintViolationWithTemplate(messageTemplate).addConstraintViolation();
      return false;
   }
}
