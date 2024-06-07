def extract_words_from_text1(text, mode=0):
    lines = text.strip().split('\n')
    words = []
    
    for line in lines:
        parts = line.split()
        if len(parts) > 1:
            if mode == 0:
                words.append(parts[1])
            else:
                words.append(parts[0])
    
    return words

def read_file_and_extract_words(filename, mode=0):
    with open(filename, 'r') as file:
        text = file.read()
    
    words = extract_words_from_text1(text, mode)
    return words

def find_unique_words(list1, list2):
    set1 = set(list1)
    set2 = set(list2)
    
    unique_to_list1 = set1 - set2
    
    unique_to_list2 = set2 - set1
    
    return list(unique_to_list1), list(unique_to_list2)

# Example usage:
wanted_file = 'wanted.txt'
wanted_words = read_file_and_extract_words(wanted_file, mode=0)

result_file = 'result.txt'
result_words = read_file_and_extract_words(result_file, mode=1)

print(find_unique_words(wanted_words, result_words))
