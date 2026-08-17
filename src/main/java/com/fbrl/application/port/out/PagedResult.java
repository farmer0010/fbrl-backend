package com.fbrl.application.port.out;

import java.util.List;

public record PagedResult<T>(List<T> items, long totalElements) {}
