package com.nexus.platform.controller;

import com.nexus.platform.dto.DiscoverFeedItem;
import com.nexus.platform.dto.DiscoverHomeResponse;
import com.nexus.platform.dto.Result;
import com.nexus.platform.service.DiscoverService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/discover")
@RequiredArgsConstructor
public class DiscoverController {
    private final DiscoverService discoverService;

    @GetMapping("/feed")
    public Result<List<DiscoverFeedItem>> feed(@RequestParam(defaultValue = "20") int limit) {
        return discoverService.getFeed(limit);
    }

    @GetMapping("/home")
    public Result<DiscoverHomeResponse> home(@RequestParam(defaultValue = "20") int limit) {
        return discoverService.getHome(limit);
    }
}
