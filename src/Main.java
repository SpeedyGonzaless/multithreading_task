import model.CalculateResult;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Main {

	public static void main(String[] args) throws ExecutionException, InterruptedException {

		Calendar start = Calendar.getInstance();

		int TASK_NUMBER = 1000;

		ExecutorService executorDownload = Executors.newFixedThreadPool(10);
		ExecutorService executorCalculate = Executors.newFixedThreadPool(20);


		List<CompletableFuture<CalculateResult>> futureCalculateResults = IntStream.range(0, TASK_NUMBER)
				.mapToObj(i -> CompletableFuture.supplyAsync(() -> new Download(i), executorDownload)
						.thenApplyAsync(Main::calculateResultFuture, executorCalculate))
				.collect(Collectors.toList());

		CompletableFuture<Long> countFuture = CompletableFuture
				.allOf(futureCalculateResults.toArray(new CompletableFuture[0]))
				.thenApply(v ->
						futureCalculateResults.stream()
								.map(CompletableFuture::join)
								.collect(Collectors.toList()))
				.thenApply(calculateResults -> calculateResults.stream()
						.filter(calculateResult -> calculateResult.found)
						.count());

		executorDownload.shutdown();
		executorCalculate.shutdown();

		System.out.println("Total success checks: " + countFuture.get());

		Calendar stop = Calendar.getInstance();

		System.out.println("Total time: " + (stop.getTimeInMillis() - start.getTimeInMillis()) + " ms");

	}

	public static CalculateResult calculateResultFuture(Download download) {
		return new Calculate((download.call())).call();
	}
}