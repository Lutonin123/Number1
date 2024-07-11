package com.example.demo;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/bitbucket")
public class BitbucketController {

	private final BitbucketService bitbucketService;
	
	private WebClient webClient = WebClient.create();

	@Autowired
	public BitbucketController(BitbucketService bitbucketService) {
		this.bitbucketService = bitbucketService;
	}

	@GetMapping("/getRepos")
	public Mono<String> getReposBitbucket() {
		return bitbucketService.getRepos();
	}

	@PostMapping("/updateLocalRepo")
	public String updateLocalRepo(@RequestParam String remoteUrl, @RequestParam String localPath) {
		try {
			return (bitbucketService.updateLocalRepo(remoteUrl, localPath));
		} catch (Exception e) {
			return "Ошибка: " + e.getMessage();
		}
	}

	@PostMapping("/updateRemoteRepo")
	public String updateRemoteRepo(@RequestParam String localPath) {
		try {
			return bitbucketService.updateRemoteRepo(localPath);
		} catch (Exception e) {
			return "Ошибка: " + e.getMessage();
		}
	}

	@PostMapping("/updateAllRemoteRepo")
	public String updateAllRemoteRepo(@RequestParam String localPath) {
		try {
			return (bitbucketService.updateAllRemoteRepo(localPath));
		} catch (Exception e) {
			return "Ошибка: " + e.getMessage();
		}
	}

	@PostMapping("/updateAllLocalRepo")
	public String updateAllLocalRepo(@RequestParam String localPath) {
		try {
			String result = bitbucketService.updateAllLocalRepo(localPath);
			if (result.equals("Перезапуск")) {
				webClient.post().uri("http://localhost:8080/bitbucket/updateAllLocalRepo?localPath=" + localPath).retrieve().bodyToMono(String.class).subscribe();
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
			return bitbucketService.transfer();
		} catch (Exception e) {
			return "Ошибка: " + e.getMessage();
		}
	}
}
