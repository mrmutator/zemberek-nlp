package zemberek.morphology.analyzer;

import static zemberek.core.turkish.PhoneticAttribute.FirstLetterConsonant;
import static zemberek.core.turkish.PhoneticAttribute.FirstLetterVowel;
import static zemberek.core.turkish.PhoneticAttribute.HasNoVowel;
import static zemberek.core.turkish.PhoneticAttribute.LastLetterConsonant;
import static zemberek.core.turkish.PhoneticAttribute.LastLetterVoiceless;
import static zemberek.core.turkish.PhoneticAttribute.LastLetterVowel;
import static zemberek.core.turkish.PhoneticAttribute.LastVowelBack;
import static zemberek.core.turkish.PhoneticAttribute.LastVowelFrontal;
import static zemberek.core.turkish.PhoneticAttribute.LastVowelRounded;
import static zemberek.core.turkish.PhoneticAttribute.LastVowelUnrounded;
import static zemberek.core.turkish.TurkishAlphabet.L_a;
import static zemberek.core.turkish.TurkishAlphabet.L_e;
import static zemberek.core.turkish.TurkishAlphabet.L_i;
import static zemberek.core.turkish.TurkishAlphabet.L_ii;
import static zemberek.core.turkish.TurkishAlphabet.L_u;
import static zemberek.core.turkish.TurkishAlphabet.L_uu;
import static zemberek.morphology.structure.Turkish.Alphabet;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import zemberek.core.turkish.PhoneticAttribute;
import zemberek.core.turkish.TurkicLetter;
import zemberek.core.turkish.TurkishLetterSequence;
import zemberek.morphology.morphotactics.MorphemeTransition;
import zemberek.morphology.morphotactics.SuffixTransition;

public class MorphemeSurfaceForm {

  public final String surface;
  public final MorphemeTransition lexicalTransition;

  public MorphemeSurfaceForm(String surface, SuffixTransition suffixTransition) {
    this.surface = surface;
    this.lexicalTransition = suffixTransition;
  }

  @Override
  public String toString() {
    return lexicalTransition.to.id + ":" + (surface.isEmpty() ? "ε" : surface);
  }

  public static TurkishLetterSequence generate(
      List<SuffixTemplateToken> tokens,
      EnumSet<PhoneticAttribute> phoneticAttributes) {

    TurkishLetterSequence seq = new TurkishLetterSequence();
    int index = 0;
    for (SuffixTemplateToken token : tokens) {
      EnumSet<PhoneticAttribute> formAttrs = defineMorphemicAttributes(seq, phoneticAttributes);
      switch (token.type) {
        case LETTER:
          seq.append(token.letter);
          break;

        case A_WOVEL:
          if (index == 0 && phoneticAttributes.contains(LastLetterVowel)) {
            break;
          }
          TurkicLetter lA = TurkicLetter.UNDEFINED;
          if (formAttrs.contains(LastVowelBack)) {
            lA = L_a;
          } else if (formAttrs.contains(LastVowelFrontal)) {
            lA = L_e;
          }
          if (lA == TurkicLetter.UNDEFINED) {
            throw new IllegalArgumentException("Cannot generate A form!");
          }
          seq.append(lA);
          break;

        case I_WOVEL:
          if (index == 0 && phoneticAttributes.contains(LastLetterVowel)) {
            break;
          }
          TurkicLetter li = TurkicLetter.UNDEFINED;
          if (formAttrs.containsAll(Arrays.asList(LastVowelBack, LastVowelRounded))) {
            li = L_u;
          } else if (formAttrs.containsAll(Arrays.asList(LastVowelBack, LastVowelUnrounded))) {
            li = L_ii;
          } else if (formAttrs.containsAll(Arrays.asList(LastVowelFrontal, LastVowelRounded))) {
            li = L_uu;
          } else if (formAttrs.containsAll(Arrays.asList(LastVowelFrontal, LastVowelUnrounded))) {
            li = L_i;
          }
          if (li == TurkicLetter.UNDEFINED) {
            throw new IllegalArgumentException("Cannot generate I form!");
          }
          seq.append(li);
          break;

        case APPEND:
          if (formAttrs.contains(LastLetterVowel)) {
            seq.append(token.letter);
          }
          break;

        case DEVOICE_FIRST:
          TurkicLetter ld = token.letter;
          if (formAttrs.contains(LastLetterVoiceless)) {
            ld = Alphabet.devoice(token.letter);
          }
          seq.append(ld);
          break;

        case LAST_VOICED:
        case LAST_NOT_VOICED:
          ld = token.letter;
          seq.append(ld);
          break;
      }
      index++;
    }
    return seq;

  }


  // in suffix, defining morphemic attributes is straight forward.
  // TODO: move this to somewhere more general.
  public static EnumSet<PhoneticAttribute> defineMorphemicAttributes(
      TurkishLetterSequence seq,
      EnumSet<PhoneticAttribute> predecessorAttrs) {
    if (seq.length() == 0) {
      return EnumSet.copyOf(predecessorAttrs);
    }
    EnumSet<PhoneticAttribute> attrs = EnumSet.noneOf(PhoneticAttribute.class);
    if (seq.hasVowel()) {
      if (seq.lastVowel().isFrontal()) {
        attrs.add(LastVowelFrontal);
      } else {
        attrs.add(LastVowelBack);
      }
      if (seq.lastVowel().isRounded()) {
        attrs.add(LastVowelRounded);
      } else {
        attrs.add(LastVowelUnrounded);
      }
      if (seq.lastLetter().isVowel()) {
        attrs.add(LastLetterVowel);
      } else {
        attrs.add(LastLetterConsonant);
      }
      if (seq.firstLetter().isVowel()) {
        attrs.add(FirstLetterVowel);
      } else {
        attrs.add(FirstLetterConsonant);
      }
    } else {
      // we transfer vowel attributes from the predecessor attributes.
      attrs = EnumSet.copyOf(predecessorAttrs);
      attrs.addAll(Arrays.asList(LastLetterConsonant, FirstLetterConsonant, HasNoVowel));
      attrs.remove(LastLetterVowel);
    }
    if (seq.lastLetter().isVoiceless()) {
      attrs.add(PhoneticAttribute.LastLetterVoiceless);
      if (seq.lastLetter().isStopConsonant()) {
        // kitap
        attrs.add(PhoneticAttribute.LastLetterVoicelessStop);
      }
    } else {
      attrs.add(PhoneticAttribute.LastLetterVoiced);
    }
    return attrs;
  }

  EnumSet<PhoneticAttribute> defineMorphemicAttributes(TurkishLetterSequence seq) {
    return defineMorphemicAttributes(seq, EnumSet.noneOf(PhoneticAttribute.class));
  }

  public enum TemplateTokenType {
    I_WOVEL,
    A_WOVEL,
    DEVOICE_FIRST,
    //VOICE_LAST,
    LAST_VOICED,
    LAST_NOT_VOICED,
    APPEND,
    LETTER
  }

  public static class SuffixTemplateToken {

    TemplateTokenType type;
    TurkicLetter letter;
    boolean append = false;

    private SuffixTemplateToken(TemplateTokenType type, TurkicLetter letter) {
      this.type = type;
      this.letter = letter;
    }

    private SuffixTemplateToken(TemplateTokenType type, TurkicLetter letter, boolean append) {
      this.type = type;
      this.letter = letter;
      this.append = append;
    }
  }

  public static class SuffixTemplateTokenizer implements Iterator<SuffixTemplateToken> {

    private final String generationWord;
    private int pointer;

    public SuffixTemplateTokenizer(String generationWord) {
      this.generationWord = generationWord;
    }

    public boolean hasNext() {
      return generationWord != null && pointer < generationWord.length();
    }

    public SuffixTemplateToken next() {
      if (!hasNext()) {
        throw new NoSuchElementException("no elements left!");
      }
      char c = generationWord.charAt(pointer++);
      char cNext = 0;
      if (pointer < generationWord.length()) {
        cNext = generationWord.charAt(pointer);
      }

      switch (c) {
        case '+':
          pointer++;
          if (cNext == 'I') {
            return new SuffixTemplateToken(TemplateTokenType.I_WOVEL, TurkicLetter.UNDEFINED, true);
          } else if (cNext == 'A') {
            return new SuffixTemplateToken(TemplateTokenType.A_WOVEL, TurkicLetter.UNDEFINED, true);
          } else {
            return new SuffixTemplateToken(TemplateTokenType.APPEND, Alphabet.getLetter(cNext));
          }
        case '>':
          pointer++;
          return new SuffixTemplateToken(TemplateTokenType.DEVOICE_FIRST,
              Alphabet.getLetter(cNext));
        case '~':
          pointer++;
          return new SuffixTemplateToken(TemplateTokenType.LAST_VOICED, Alphabet.getLetter(cNext));
        case '!':
          pointer++;
          return new SuffixTemplateToken(TemplateTokenType.LAST_NOT_VOICED,
              Alphabet.getLetter(cNext));
        case 'I':
          return new SuffixTemplateToken(TemplateTokenType.I_WOVEL, TurkicLetter.UNDEFINED);
        case 'A':
          return new SuffixTemplateToken(TemplateTokenType.A_WOVEL, TurkicLetter.UNDEFINED);
        default:
          return new SuffixTemplateToken(TemplateTokenType.LETTER, Alphabet.getLetter(c));

      }
    }

    public void remove() {
    }
  }
}