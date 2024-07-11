package com.example.demo;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Base64;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

public class GitStaticMethods {

	public static void checkRemoteRepo(String remoteUrl, String login, String password) throws GitAPIException {
		Git.lsRemoteRepository().setHeads(true).setTags(true).setRemote(remoteUrl)
				.setCredentialsProvider(new UsernamePasswordCredentialsProvider(login, password)).call();
	}

	public static void clearDirectory(File directory) {
		File[] files = directory.listFiles();
		if (files != null)
			for (File file : files)
				if (file.isDirectory())
					clearDirectory(file);
				else
					file.delete();
		directory.delete();
	}

	public static Mono<String> getReposFromBitbucket(String bitbucketLogin, String bitbucketPassword,
			String bitbucketWorkspace) {
		String auth = bitbucketLogin + ":" + bitbucketPassword;
		String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
		WebClient webClient = WebClient.create("https://api.bitbucket.org/2.0/repositories/" + bitbucketWorkspace);
		return webClient.get().header("Authorization", "Basic " + encodedAuth)
				.header("Content-Type", "application/json").header("Accept", "application/json").retrieve()
				.bodyToMono(String.class);
	}

	public static Mono<String> getReposFromGitHub(String githubToken) {
		WebClient webClient = WebClient.create("https://api.github.com/user/repos");
		return webClient.get().header("Authorization", "Bearer " + githubToken)
				.header("X-GitHub-Api-Version", "2022-11-28").accept(MediaType.APPLICATION_JSON).retrieve()
				.bodyToMono(String.class);
	}

	public static void getReposToFile(Mono<String> responseMono) {
		responseMono.subscribe(responseBody -> {
			try {
				Path filePath = Paths.get("response.txt");
				Files.write(filePath, responseBody.getBytes(), StandardOpenOption.CREATE);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}
}
