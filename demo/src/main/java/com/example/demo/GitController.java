package com.example.demo;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;
@RestController
@RequestMapping("/git")
public class GitController {

	private final GitService gitHubService;

	@Autowired
	public GitController(GitService gitHubService) {
		this.gitHubService = gitHubService;
	}

	@GetMapping("/getReposGitHub")
	public Mono<String> getReposGitHub() {
		return gitHubService.getReposGitHub();
	}
	
	@GetMapping("/getReposBitbucket")
	public Mono<String> getReposBitbucket() {
		return gitHubService.getReposBitbucket();
	}

	@PostMapping("/updateLocalRepo")
	public String updateLocalRepo(@RequestParam String remoteUrl, @RequestParam String localPath,
			@RequestParam String service) {
		try {
			return (gitHubService.updateLocalRepo(remoteUrl, localPath, service));
		} catch (Exception e) {
			return "Ошибка: " + e.getMessage();
		}
	}

	@PostMapping("/updateRemoteRepo")
	public String updateRemoteRepo(@RequestParam String localPath, @RequestParam String service) {
		try {
			return gitHubService.updateRemoteRepo(localPath, service);
		} catch (Exception e) {
			return "Ошибка: " + e.getMessage();
		}
	}

	@PostMapping("/updateAllRemoteRepo")
	public String updateAllRemoteRepo(@RequestParam String localPath, @RequestParam String service) {
		try {
			return (gitHubService.updateAllRemoteRepo(localPath, service));
		} catch (Exception e) {
			return "Ошибка: " + e.getMessage();
		}
	}

	@PostMapping("/updateAllLocalRepo")
	public String updateAllLocalRepo(@RequestParam String localPath, @RequestParam String service) {
		try {
			return gitHubService.updateAllLocalRepo(localPath, service);
		} catch (Exception e) {
			return "Ошибка: " + e.getMessage();
		}
	}
	
	@PostMapping("/transfer")
	public String transfer(@RequestParam String from, @RequestParam String to) {
		try {
			return gitHubService.transfer(from, to);
		} catch (Exception e) {
			return "Ошибка: " + e.getMessage();
		}
	}
}
