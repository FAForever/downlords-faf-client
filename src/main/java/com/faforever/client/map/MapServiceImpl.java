import org.jetbrains.annotations.Nullable;

private CompletableFuture<Void> downloadAndInstallMap(String folderName, URL downloadUrl, @Nullable DoubleProperty progressProperty, @Nullable StringProperty titleProperty) {
    if (mapGeneratorService.isGeneratedMap(folderName)) {
    return mapGeneratorService.generateMap(folderName).thenRun(() -> {
    });
    }