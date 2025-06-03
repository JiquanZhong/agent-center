package com.diit.ds.web;import com.baomidou.mybatisplus.extension.plugins.pagination.Page;import com.diit.ds.pojo.entity.Agents;
import com.diit.ds.pojo.entity.AgentsTag;
import com.diit.ds.pojo.params.*;
import com.diit.ds.pojo.vo.AgentsVo;
import com.diit.ds.service.AgentsService;
import com.diit.ds.service.AgentsTagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author yjxbz
 */
@Slf4j
@RestController
@RequestMapping("/api/agent")
@Tag(name = "智能体应用查询", description = "智能体查询调用相关接口")
@RequiredArgsConstructor
public class AgentController {

    private final AgentsService agentsService;

    private final AgentsTagService agentsTagService;

//    @PostMapping("/list")
//    @Operation(summary = "智能体查询", description = "智能体查询")
//    public ResponseEntity<Page<AgentsVo>> list(@RequestBody QueryAgentsParams params) {
//        Page<AgentsVo> agentsList = agentsService.getAgentsList(params);
//        return ResponseEntity.status(HttpStatus.OK).body(agentsList);
//    }
//
//    @PostMapping("/favorite")
//    @Operation(summary = "添加/取消收藏", description = "添加/取消收藏")
//    public ResponseEntity<Boolean> favorite(@RequestBody FavoriteParams params) {
//        boolean result = agentsService.favorite(params);
//        return ResponseEntity.status(HttpStatus.OK).body(result);
//    }
//
//    @PostMapping("/getUserFavorite")
//    @Operation(summary = "获取用户收藏列表", description = "获取用户收藏列表")
//    public ResponseEntity<Page<AgentsVo>> getUserFavorite(@RequestBody GetUserFavoriteParams params) {
//        return ResponseEntity.status(HttpStatus.OK).body(agentsService.getUserFavorite(params));
//    }
//
//    @PostMapping("/getHotAgents")
//    @Operation(summary = "获取热点智能体列表", description = "获取热点智能体列表")
//    public ResponseEntity<List<AgentsVo>> getHotAgents() {
//        return ResponseEntity.status(HttpStatus.OK).body(agentsService.getHotAgents());
//    }
//
//    @PostMapping("/browse")
//    @Operation(summary = "浏览量修改", description = "浏览量修改")
//    public ResponseEntity<Boolean> browse(@RequestBody BrowseParams params) {
//        boolean result = agentsService.browse(params);
//        return ResponseEntity.status(HttpStatus.OK).body(result);
//    }
//
//    @PostMapping("/getUserAgents")
//    @Operation(summary = "获取用户个人的智能体列表", description = "获取用户个人的智能体列表")
//    public ResponseEntity<Page<AgentsVo>> getUserAgents(@RequestBody GetUserAgentsParams params) {
//        Page<AgentsVo> result = agentsService.getUserAgents(params);
//        return ResponseEntity.status(HttpStatus.OK).body(result);
//    }

    @PostMapping("/getTags")
    @Operation(summary = "获取智能体标签列表", description = "获取智能体标签列表")
    public ResponseEntity<List<AgentsTag>> getTags() {
        List<AgentsTag> result = agentsTagService.getTags();
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }
}
