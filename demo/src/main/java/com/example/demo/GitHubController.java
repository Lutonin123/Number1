package com.example.demo;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/gitHub")
public class GitHubController {

	private final GitHubService gitHubService;
	
	private WebClient webClient = WebClient.create();

	@Autowired
	public GitHubController(GitHubService gitHubService) {
		this.gitHubService = gitHubService;
	}

	@GetMapping("/getReposGitHub")
	public Mono<String> getReposGitHub() {
		return gitHubService.getReposGitHub();
	}

	@PostMapping("/updateLocalRepo")
	public String updateLocalRepo(@RequestParam String remoteUrl, @RequestParam String localPath) {
		try {
			return (gitHubService.updateLocalRepo(remoteUrl, localPath));
		} catch (Exception e) {
			return "Ошибка: " + e.getMessage();
		}
	}

	@PostMapping("/updateRemoteRepo")
	public String updateRemoteRepo(@RequestParam String localPath) {
		try {
			return gitHubService.updateRemoteRepo(localPath);
		} catch (Exception e) {
			return "Ошибка: " + e.getMessage();
		}
	}

	@PostMapping("/updateAllRemoteRepo")
	public String updateAllRemoteRepo(@RequestParam String localPath) {
		try {
			return (gitHubService.updateAllRemoteRepo(localPath));
		} catch (Exception e) {
			return "Ошибка: " + e.getMessage();
		}
	}

	@PostMapping("/updateAllLocalRepo")
	public String updateAllLocalRepo(@RequestParam String localPath) {
		try {
			String result = gitHubService.updateAllLocalRepo(localPath);
			if (result.equals("Перезапуск")) {
				webClient.post().uri("http://localhost:8080/gitHub/updateAllLocalRepo?localPath=" + localPath).retrieve().bodyToMono(String.class).subscribe();
				return "Поиск информации о репозиториях";
			}
			return "Локальные репозитории обновлены";
		} catch (Exception e) {
			return "Ошибка: " + e.getMessage();
		}
	}

	@PostMapping("/transfer")
	public String transfer() {
		try {
			return gitHubService.transfer();
		} catch (Exception e) {
			return "Ошибка: " + e.getMessage();
		}
	}
}
