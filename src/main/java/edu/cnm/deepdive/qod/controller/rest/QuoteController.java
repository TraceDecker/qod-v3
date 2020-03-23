package edu.cnm.deepdive.qod.controller.rest;

import edu.cnm.deepdive.qod.controller.exception.SearchTermTooShortException;
import edu.cnm.deepdive.qod.model.entity.Quote;
import edu.cnm.deepdive.qod.model.entity.Source;
import edu.cnm.deepdive.qod.service.QuoteRepository;
import edu.cnm.deepdive.qod.service.SourceRepository;
import java.time.LocalDate;
import java.util.Date;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/quotes")
@ExposesResourceFor(Quote.class)
public class QuoteController {

  private static final long MILLISECONDS_PER_DAY = 1000L * 60 * 60 * 24;

  private final QuoteRepository quoteRepository;
  private final SourceRepository sourceRepository;

  @Autowired
  public QuoteController(QuoteRepository quoteRepository, SourceRepository sourceRepository) {
    this.quoteRepository = quoteRepository;
    this.sourceRepository = sourceRepository;
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Quote> post(@RequestBody Quote quote) {
    Source source = quote.getSource();
    if (source != null) {
      UUID id = source.getId();
      if (id != null) {
        source = sourceRepository.findOrFail(id);
        quote.setSource(source);
      }
    }
    quoteRepository.save(quote);
    return ResponseEntity.created(quote.getHref()).body(quote);
  }

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  public Iterable<Quote> get() {
    return quoteRepository.getAllByOrderByTextAsc();
  }

  @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
  public Iterable<Quote> search(@RequestParam("q") String fragment) {
    if (fragment.length() < 3) {
      throw new SearchTermTooShortException();
    }
    return quoteRepository.getAllByTextContainsOrderByTextAsc(fragment);
  }

  @GetMapping(value = "/random", produces = MediaType.APPLICATION_JSON_VALUE)
  public Quote getRandom() {
    return quoteRepository.getRandom().get();
  }

  @GetMapping(value = "/qod", produces = MediaType.APPLICATION_JSON_VALUE)
  public Quote getQuoteOfDay(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
    long dayOffset = date.toEpochDay() % quoteRepository.getCount();
    return quoteRepository.getQuoteOfDay(dayOffset).get();
  }

  @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  public Quote get(@PathVariable UUID id) {
    return quoteRepository.findOrFail(id);
  }

  @PutMapping(value = "/{id}",
      consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public Quote put(@PathVariable UUID id, @RequestBody Quote modifiedQuote) {
    Quote quote = quoteRepository.findOrFail(id);
    quote.setText(modifiedQuote.getText());

    Source source = modifiedQuote.getSource();
    if (source != null) {
      UUID sourceId = source.getId();
      if (sourceId != null) {
        source = sourceRepository.findOrFail(sourceId);
      }
    }
    quote.setSource(source);
    return quoteRepository.save(quote);
  }

  @PutMapping(value = "/{id}/text",
      consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
  public String put(@PathVariable UUID id, @RequestBody String modifiedQuote) {
    Quote quote = quoteRepository.findOrFail(id);
    quote.setText(modifiedQuote);
    quoteRepository.save(quote);
    return quote.getText();
  }

  @DeleteMapping(value = "/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID id) {
    quoteRepository.findById(id).ifPresent(quoteRepository::delete);
  }

  @PutMapping(value = "/{quoteId}/source/{sourceId}", produces = MediaType.APPLICATION_JSON_VALUE)
  public Quote attach(@PathVariable UUID quoteId, @PathVariable UUID sourceId) {
    Quote quote = quoteRepository.findOrFail(quoteId);
    Source source = sourceRepository.findOrFail(sourceId);
    if (!source.equals(quote.getSource())) {
      quote.setSource(source);
      quoteRepository.save(quote);
    }
    return quote;
  }

  @PutMapping(value = "/{quoteId}/source",
      consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public Quote attach(@PathVariable UUID quoteId, @RequestBody Source source) {
    Quote quote = quoteRepository.findOrFail(quoteId);
    source = sourceRepository.findOrFail(source.getId());
    if (!source.equals(quote.getSource())) {
      quote.setSource(source);
      quoteRepository.save(quote);
    }
    return quote;
  }

  @DeleteMapping(value = "/{quoteId}/source/{sourceId}")
  public Quote detach(@PathVariable UUID quoteId, @PathVariable UUID sourceId) {
    Quote quote = quoteRepository.findOrFail(quoteId);
    Source source = sourceRepository.findOrFail(sourceId);
    if (source.equals(quote.getSource())) {
      quote.setSource(null);
      quoteRepository.save(quote);
    }
    return quote;
  }

  @DeleteMapping(value = "/{quoteId}/source")
  public Quote clearSource(@PathVariable UUID quoteId) {
    Quote quote = quoteRepository.findOrFail(quoteId);
    quote.setSource(null);
    return quoteRepository.save(quote);
  }

}
