package com.diit.ds.task;

import com.diit.ds.domain.dto.KnowledgeTreeNodeDTO;
import com.diit.ds.service.KnowledgeTreeNodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 知识树节点文档数量更新定时任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentNumUpdateTask implements ApplicationRunner {

    private final KnowledgeTreeNodeService knowledgeTreeNodeService;

    /**
     * 每天凌晨2点执行一次完整更新
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void scheduledUpdateDocumentNum() {
        log.info("开始执行知识树节点文档数量定时更新任务");
        try {
            // 更新所有实际节点的文档数量
            knowledgeTreeNodeService.updateAllNodesDocumentNum();
            
            // 获取树结构，这会触发虚拟根节点文档数量的计算
            knowledgeTreeNodeService.getTreeNodeDTO();
            
            log.info("知识树节点文档数量定时更新任务执行完成");
        } catch (Exception e) {
            log.error("知识树节点文档数量定时更新任务执行失败", e);
        }
    }
    
    /**
     * 应用启动时异步执行一次更新
     */
    @Override
    @Async
    public void run(ApplicationArguments args) {
        log.info("应用启动，开始执行知识树节点文档数量初始化更新");
        try {
            // 等待3分钟后执行，确保所有服务都已经启动完成
            Thread.sleep(180000);
            
            // 更新所有实际节点的文档数量
            knowledgeTreeNodeService.updateAllNodesDocumentNum();
            
            // 获取树结构，这会触发虚拟根节点文档数量的计算
            knowledgeTreeNodeService.getTreeNodeDTO();
            
            log.info("知识树节点文档数量初始化更新完成");
        } catch (Exception e) {
            log.error("知识树节点文档数量初始化更新失败", e);
        }
    }
} 