package com._37coins.workflow.pojo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class IncompleteException extends Exception {
	private static final long serialVersionUID = 1L;

}
