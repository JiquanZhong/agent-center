package com.diit.ds.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Tag(name = "外部知识库的Controller，提给给Dify的知识库接口")
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class KnowledgeDBController {


}
