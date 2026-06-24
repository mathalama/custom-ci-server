package dev.mathalama.rabotyagaci.project.controller;

import dev.mathalama.rabotyagaci.build.domain.Build;
import dev.mathalama.rabotyagaci.build.domain.BuildStatus;
import dev.mathalama.rabotyagaci.build.repository.BuildRepository;
import dev.mathalama.rabotyagaci.project.domain.Project;
import dev.mathalama.rabotyagaci.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class BadgeController {

    private final ProjectRepository projectRepository;
    private final BuildRepository buildRepository;

    @GetMapping(value = "/{id}/badge", produces = "image/svg+xml")
    public ResponseEntity<String> getProjectBadge(@PathVariable Long id) {
        Project project = projectRepository.findById(id).orElse(null);
        if (project == null) {
            return new ResponseEntity<>(generateSvg("build", "unknown project", "#9f9f9f"), HttpStatus.NOT_FOUND);
        }

        Page<Build> recentBuilds = buildRepository.findByProjectId(id, PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "createdAt")));
        
        if (recentBuilds.isEmpty()) {
            return ResponseEntity.ok()
                    .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                    .body(generateSvg("build", "no builds", "#9f9f9f"));
        }

        Build latestBuild = recentBuilds.getContent().get(0);
        String color;
        String text;

        if (latestBuild.getStatus() == BuildStatus.SUCCESS) {
            color = "#4c1";
            text = "passing";
        } else if (latestBuild.getStatus() == BuildStatus.FAILURE) {
            color = "#e05d44";
            text = "failing";
        } else if (latestBuild.getStatus() == BuildStatus.RUNNING || latestBuild.getStatus() == BuildStatus.PENDING) {
            color = "#dfb317";
            text = "running";
        } else {
            color = "#9f9f9f";
            text = latestBuild.getStatus().name().toLowerCase();
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .body(generateSvg("build", text, color));
    }

    private String generateSvg(String leftText, String rightText, String color) {
        // A simple flat SVG badge similar to Shields.io
        int leftWidth = leftText.length() * 7 + 10;
        int rightWidth = rightText.length() * 7 + 10;
        int totalWidth = leftWidth + rightWidth;

        return String.format(
            "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"%d\" height=\"20\" role=\"img\" aria-label=\"%s: %s\">\n" +
            "  <title>%s: %s</title>\n" +
            "  <linearGradient id=\"s\" x2=\"0\" y2=\"100%%\">\n" +
            "    <stop offset=\"0\" stop-color=\"#bbb\" stop-opacity=\".1\"/>\n" +
            "    <stop offset=\"1\" stop-opacity=\".1\"/>\n" +
            "  </linearGradient>\n" +
            "  <clipPath id=\"r\">\n" +
            "    <rect width=\"%d\" height=\"20\" rx=\"3\" fill=\"#fff\"/>\n" +
            "  </clipPath>\n" +
            "  <g clip-path=\"url(#r)\">\n" +
            "    <rect width=\"%d\" height=\"20\" fill=\"#555\"/>\n" +
            "    <rect x=\"%d\" width=\"%d\" height=\"20\" fill=\"%s\"/>\n" +
            "    <rect width=\"%d\" height=\"20\" fill=\"url(#s)\"/>\n" +
            "  </g>\n" +
            "  <g fill=\"#fff\" text-anchor=\"middle\" font-family=\"Verdana,Geneva,DejaVu Sans,sans-serif\" text-rendering=\"geometricPrecision\" font-size=\"110\">\n" +
            "    <text aria-hidden=\"true\" x=\"%d\" y=\"150\" fill=\"#010101\" fill-opacity=\".3\" transform=\"scale(.1)\" textLength=\"%d\">%s</text>\n" +
            "    <text x=\"%d\" y=\"140\" transform=\"scale(.1)\" fill=\"#fff\" textLength=\"%d\">%s</text>\n" +
            "    <text aria-hidden=\"true\" x=\"%d\" y=\"150\" fill=\"#010101\" fill-opacity=\".3\" transform=\"scale(.1)\" textLength=\"%d\">%s</text>\n" +
            "    <text x=\"%d\" y=\"140\" transform=\"scale(.1)\" fill=\"#fff\" textLength=\"%d\">%s</text>\n" +
            "  </g>\n" +
            "</svg>",
            totalWidth, leftText, rightText, leftText, rightText,
            totalWidth, leftWidth, leftWidth, rightWidth, color, totalWidth,
            (leftWidth * 10) / 2, (leftWidth - 10) * 10, leftText,
            (leftWidth * 10) / 2, (leftWidth - 10) * 10, leftText,
            leftWidth * 10 + (rightWidth * 10) / 2, (rightWidth - 10) * 10, rightText,
            leftWidth * 10 + (rightWidth * 10) / 2, (rightWidth - 10) * 10, rightText
        );
    }
}
